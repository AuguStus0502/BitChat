package com.bitchat.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An emergency SOS distress beacon transmitted over the BitChat mesh network.
 *
 * SOS beacons are high-priority messages designed to propagate rapidly through the mesh,
 * alerting nearby peers to an emergency. They carry optional GPS coordinates and a
 * human-readable description of the sender's condition.
 *
 * Propagation rules:
 * - Beacons are re-broadcast by every receiving peer (unless [relayPermission] is false).
 * - [ttl] limits the maximum number of relay hops to prevent infinite broadcast storms.
 * - [expiryTime] ensures stale beacons are automatically purged from the network.
 * - [isActive] can be set to false to signal that the emergency has been resolved.
 *
 * @property beaconId Unique identifier for this SOS event.
 * @property senderId Peer ID of the device that originated the beacon.
 * @property senderName Display name of the sender at time of broadcast.
 * @property priority Urgency level of the distress signal.
 * @property condition Short description of the emergency (e.g., "Injury", "Lost", "Medical").
 * @property message Free-form text with additional details about the situation.
 * @property latitude GPS latitude of the sender, or null if unavailable.
 * @property longitude GPS longitude of the sender, or null if unavailable.
 * @property timestamp Creation timestamp (ms since epoch).
 * @property expiryTime Absolute timestamp (ms) after which this beacon is no longer forwarded.
 * @property ttl Maximum number of relay hops; decremented at each hop.
 * @property hopCount Number of hops this beacon has already traversed.
 * @property relayedBy Peer ID of the node that last forwarded this beacon; null if sent directly.
 * @property isActive Whether the emergency is still ongoing; set to false to cancel.
 * @property relayPermission Whether receiving peers should re-broadcast this beacon.
 */
@Entity(tableName = "sos_beacons")
data class SosBeacon(
    @PrimaryKey val beaconId: String,
    val senderId: String,
    val senderName: String,
    val priority: SosPriority,
    val condition: String,
    val message: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val expiryTime: Long,
    val ttl: Int = 5,
    val hopCount: Int = 0,
    val relayedBy: String? = null,
    val isActive: Boolean = true,
    val relayPermission: Boolean = true
)

/**
 * Urgency classification for a [SosBeacon].
 *
 * Higher priority beacons are displayed more prominently in the UI and may trigger
 * notification sounds or vibration patterns.
 */
enum class SosPriority {
    /** Life-threatening emergency requiring immediate attention. */
    CRITICAL,

    /** Non-critical situation where assistance would be helpful. */
    HELP_NEEDED,

    /** Informational update confirming the sender is safe. */
    STABLE
}
