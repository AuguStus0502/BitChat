package com.bitchat.network.sos

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.bitchat.core.models.SosBeacon
import com.bitchat.core.models.SosPriority
import com.bitchat.core.protocol.Packet
import com.bitchat.core.protocol.PacketType
import com.bitchat.core.protocol.ProtocolConstants
import com.bitchat.core.utils.IdGenerator
import com.bitchat.network.routing.MessageRelay
import com.bitchat.security.identity.IdentityManager
import com.bitchat.storage.repositories.SosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages SOS beacon lifecycle: creation, broadcasting, receiving, relaying, and expiration.
 *
 * In Disaster Resilience mode, this is the core component that enables emergency
 * communication. SOS beacons are broadcast to all nearby peers via the BLE mesh
 * and relayed with TTL decrementation until they expire or reach max hops.
 *
 * The manager integrates with [MessageRelay] for mesh transmission, [SosRepository]
 * for persistence, and Android's built-in [LocationManager] for GPS coordinates.
 *
 * Security considerations:
 * - SOS beacons are intentionally unencrypted for maximum reach
 * - GPS precision is reduced to ~100m to protect exact location
 * - Beacons expire after [ProtocolConstants.SOS_MESSAGE_EXPIRY_MS]
 * - Relay permission can be revoked by the sender
 */
class SosManager(
    private val context: Context,
    private val identityManager: IdentityManager,
    private val sosRepository: SosRepository,
    private val messageRelay: MessageRelay?
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** All currently active (non-expired) SOS beacons observed from the mesh. */
    private val _activeBeacons = MutableStateFlow<List<SosBeacon>>(emptyList())
    val activeBeacons: StateFlow<List<SosBeacon>> = _activeBeacons.asStateFlow()

    /** True while the local device is broadcasting an SOS beacon. */
    private val _isBroadcasting = MutableStateFlow(false)
    val isBroadcasting: StateFlow<Boolean> = _isBroadcasting.asStateFlow()

    /** The locally-originated SOS beacon, if one is currently active. */
    private val _localBeacon = MutableStateFlow<SosBeacon?>(null)
    val localBeacon: StateFlow<SosBeacon?> = _localBeacon.asStateFlow()

    init {
        scope.launch {
            sosRepository.observeActiveBeacons().collect { beacons ->
                _activeBeacons.value = beacons
            }
        }
    }

    /**
     * Compose and broadcast a new SOS beacon.
     *
     * @param priority Emergency priority level.
     * @param condition Short description of the sender's condition.
     * @param message Free-form message for rescuers.
     * @param includeLocation Whether to attach GPS coordinates.
     * @param allowRelay Whether peers should relay this beacon.
     * @return The created [SosBeacon] that was broadcast.
     */
    suspend fun broadcastSos(
        priority: SosPriority,
        condition: String,
        message: String,
        includeLocation: Boolean = true,
        allowRelay: Boolean = true
    ): SosBeacon {
        val identity = identityManager.getIdentity()
        val location = if (includeLocation) getLastLocation() else null

        val beacon = SosBeacon(
            beaconId = IdGenerator.generateId(),
            senderId = identity.identityId,
            senderName = identity.displayName,
            priority = priority,
            condition = condition,
            message = message,
            latitude = location?.latitude?.let { truncateCoordinate(it) },
            longitude = location?.longitude?.let { truncateCoordinate(it) },
            timestamp = System.currentTimeMillis(),
            expiryTime = System.currentTimeMillis() + ProtocolConstants.SOS_MESSAGE_EXPIRY_MS,
            ttl = ProtocolConstants.DEFAULT_TTL,
            hopCount = 0,
            relayedBy = null,
            isActive = true,
            relayPermission = allowRelay
        )

        sosRepository.saveBeacon(beacon)
        _localBeacon.value = beacon
        _isBroadcasting.value = true

        messageRelay?.broadcastSos(
            content = message,
            senderId = identity.identityId,
            senderName = identity.displayName,
            priority = priority.name,
            condition = condition,
            ttl = ProtocolConstants.DEFAULT_TTL
        )

        return beacon
    }

    /**
     * Process an incoming SOS beacon received from the BLE mesh.
     *
     * Deduplicates against existing beacons, persists the new beacon,
     * and optionally relays it further if TTL > 0 and relay is permitted.
     *
     * @param beacon The received SOS beacon.
     */
    suspend fun receiveSosBeacon(beacon: SosBeacon) {
        val existing = sosRepository.getBeaconById(beacon.beaconId)
        if (existing != null) return

        if (System.currentTimeMillis() > beacon.expiryTime) return

        val localIdentity = identityManager.getIdentity()
        if (beacon.senderId == localIdentity.identityId) return

        sosRepository.saveBeacon(beacon)

        if (beacon.relayPermission && beacon.ttl > 1) {
            val relayedBeacon = beacon.copy(
                ttl = beacon.ttl - 1,
                hopCount = beacon.hopCount + 1,
                relayedBy = localIdentity.identityId
            )
            messageRelay?.broadcastSos(
                content = relayedBeacon.message,
                senderId = relayedBeacon.senderId,
                senderName = relayedBeacon.senderName,
                priority = relayedBeacon.priority.name,
                condition = relayedBeacon.condition,
                ttl = relayedBeacon.ttl
            )
        }
    }

    /**
     * Deactivate a locally-originated SOS beacon.
     *
     * Sends a cancellation to the mesh and updates local state.
     *
     * @param beaconId The ID of the beacon to deactivate.
     */
    suspend fun cancelBeacon(beaconId: String) {
        val beacon = sosRepository.getBeaconById(beaconId) ?: return
        val cancelled = beacon.copy(isActive = false, ttl = 0)
        sosRepository.updateBeacon(cancelled)

        if (_localBeacon.value?.beaconId == beaconId) {
            _localBeacon.value = null
            _isBroadcasting.value = false
        }
    }

    /** Clean up expired beacons from the database. */
    suspend fun cleanupExpired() {
        sosRepository.deactivateExpired()
    }

    /** Release resources. */
    fun destroy() {
        scope.cancel()
    }

    /**
     * Get the last known GPS location from the Android LocationManager.
     *
     * Uses the network provider as a fallback when GPS is unavailable.
     * Requires ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION permission.
     *
     * @return The last known [Location], or null if unavailable.
     */
    @SuppressLint("MissingPermission")
    private fun getLastLocation(): Location? {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            locationManager?.let {
                val providers = it.getProviders(true)
                var bestLocation: Location? = null
                for (provider in providers) {
                    val location = it.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                        bestLocation = location
                    }
                }
                bestLocation
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Truncate GPS coordinates to ~100m precision to protect exact location.
     * Approximately 3 decimal places gives ~110m precision.
     */
    private fun truncateCoordinate(value: Double): Double {
        return Math.round(value * 1000.0) / 1000.0
    }

    companion object {
        @Volatile
        private var instance: SosManager? = null

        fun getInstance(
            context: Context,
            identityManager: IdentityManager,
            sosRepository: SosRepository,
            messageRelay: MessageRelay?
        ): SosManager {
            return instance ?: synchronized(this) {
                instance ?: SosManager(
                    context.applicationContext,
                    identityManager,
                    sosRepository,
                    messageRelay
                ).also { instance = it }
            }
        }
    }
}
