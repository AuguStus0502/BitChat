package com.bitchat.ui.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bitchat.app.BitChatApplication
import com.bitchat.network.ble.BleConnectionManager
import com.bitchat.security.identity.IdentityManager
import com.bitchat.storage.repositories.PeerRepository
import com.bitchat.storage.repositories.SosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel powering the [HomeScreen][com.bitchat.ui.screens.home.HomeScreen] with live
 * identity, peer-count, SOS, and battery data.
 *
 * Exposes five read-only [StateFlow] properties that the Compose UI can collect:
 * - [identityName] — the local user's display name from [IdentityManager].
 * - [connectedPeerCount] — number of peers in a connected/authenticated state.
 * - [isBluetoothEnabled] — whether the device's Bluetooth adapter is on.
 * - [batteryPercent] — current battery level (0-100).
 * - [hasActiveSos] — whether the local user is currently broadcasting an SOS beacon.
 *
 * ### Dependency Wiring
 * All dependencies are constructed from the application-scoped
 * [BitChatApplication.database] — no DI framework is required.
 *
 * ### Thread Safety
 * [MutableStateFlow] is thread-safe by design. Battery broadcasts are received on the main
 * thread via a registered [BroadcastReceiver] and emitted into the flow. BLE and database
 * reads happen on [viewModelScope] (which defaults to [Dispatchers.Main][kotlinx.coroutines.Dispatchers.Main]);
 * database and repository calls are suspended and execute on Room's internal dispatcher.
 *
 * @param application The running [Application] instance, used to access system services
 *                    and the shared [BitChatApplication.database].
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // ── Dependency references ────────────────────────────────────────────

    /** Shared application instance, cast to [BitChatApplication] for database access. */
    private val app = application as BitChatApplication

    /** Manages the local cryptographic identity (display name, key pair). */
    private val identityManager = IdentityManager(application)

    /** Repository for querying peer connection state from the Room database. */
    private val peerRepository = PeerRepository(app.database.peerDao())

    /** Repository for observing SOS beacon state from the Room database. */
    private val sosRepository = SosRepository(app.database.sosBeaconDao())

    /** BLE connection manager used for the active-connection count metric. */
    private val bleConnectionManager = BleConnectionManager(application)

    /** Android system BluetoothManager for adapter-level state queries. */
    private val bluetoothManager =
        application.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    // ── Mutable backing fields ───────────────────────────────────────────

    /**
     * The local user's display name, initialised to an empty string and
     * populated once [IdentityManager.getIdentity] completes.
     */
    private val _identityName = MutableStateFlow("")

    /**
     * Number of peers currently in a CONNECTED or AUTHENTICATED state.
     * Updated whenever the Room-backed peer list changes.
     */
    private val _connectedPeerCount = MutableStateFlow(0)

    /**
     * Reflects whether the device's Bluetooth adapter is currently enabled.
     * Updated on construction and whenever the system broadcasts a state change.
     */
    private val _isBluetoothEnabled = MutableStateFlow(false)

    /**
     * Current battery capacity as a percentage (0–100).
     * Queried on construction and refreshed via [batteryReceiver].
     */
    private val _batteryPercent = MutableStateFlow(0)

    /**
     * True when the local user has an active (non-expired) SOS beacon in the database.
     * Derived from [sosRepository] observations filtered by the local identity ID.
     */
    private val _hasActiveSos = MutableStateFlow(false)

    // ── Public read-only StateFlows ──────────────────────────────────────

    /** Observable display name of the local identity. */
    val identityName: StateFlow<String> = _identityName.asStateFlow()

    /** Observable count of directly connected peers. */
    val connectedPeerCount: StateFlow<Int> = _connectedPeerCount.asStateFlow()

    /** Observable Bluetooth adapter enabled state. */
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    /** Observable battery percentage (0–100). */
    val batteryPercent: StateFlow<Int> = _batteryPercent.asStateFlow()

    /** Observable flag indicating an active local SOS beacon. */
    val hasActiveSos: StateFlow<Boolean> = _hasActiveSos.asStateFlow()

    // ── BroadcastReceiver for battery changes ───────────────────────────

    /**
     * Receives [Intent.ACTION_BATTERY_CHANGED] broadcasts and updates
     * [_batteryPercent] with the latest capacity value.
     */
    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (scale > 0) {
                _batteryPercent.value = (level * 100) / scale
            }
        }
    }

    // ── Initialisation ───────────────────────────────────────────────────

    init {
        // Query the current Bluetooth adapter state
        _isBluetoothEnabled.value = bluetoothManager?.adapter?.isEnabled == true

        // Query initial battery level from the sticky broadcast
        queryBatteryLevel()

        // Register for future battery change broadcasts
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        application.registerReceiver(batteryReceiver, filter)

        // Load identity name asynchronously
        viewModelScope.launch {
            val identity = identityManager.getIdentity()
            _identityName.value = identity.displayName
        }

        // Observe connected peer count from the Room database
        viewModelScope.launch {
            peerRepository.observeConnectedCount().collect { count ->
                _connectedPeerCount.value = count
            }
        }

        // Observe active beacons and derive hasActiveSos for the local identity
        viewModelScope.launch {
            val localIdentity = identityManager.getIdentity()
            sosRepository.observeActiveBeacons().collect { beacons ->
                _hasActiveSos.value = beacons.any { beacon ->
                    beacon.senderId == localIdentity.identityId && beacon.isActive
                }
            }
        }
    }

    // ── Public helpers ───────────────────────────────────────────────────

    /**
     * Refresh the Bluetooth adapter enabled state.
     *
     * Should be called from the UI layer when the screen resumes or after
     * a Bluetooth settings change, since the BroadcastReceiver above only
     * tracks battery, not Bluetooth.
     */
    fun refreshBluetoothState() {
        _isBluetoothEnabled.value = bluetoothManager?.adapter?.isEnabled == true
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Read the current battery level from the sticky [ACTION_BATTERY_CHANGED]
     * broadcast. This avoids waiting for the next broadcast to populate the UI.
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun queryBatteryLevel() {
        val batteryIntent = getApplication<BitChatApplication>().registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (scale > 0) {
            _batteryPercent.value = (level * 100) / scale
        }
    }

    // ── Cleanup ──────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        // Unregister the battery receiver to prevent leaks
        try {
            getApplication<BitChatApplication>().unregisterReceiver(batteryReceiver)
        } catch (_: IllegalArgumentException) {
            // Receiver was already unregistered — safe to ignore
        }
    }
}
