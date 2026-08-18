package com.bitchat.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a discovered Bluetooth peer device on the BitChat mesh network.
 *
 * Peers are the fundamental unit of the mesh topology. Each peer is identified by a unique
 * [peerId] and tracked through its lifecycle from initial BLE discovery, through connection
 * and authentication, to active message relay. Peers are persisted to a local Room database
 * so they survive app restarts.
 *
 * Key relationships:
 * - A peer may act as a [isRelay] node, forwarding messages for other peers in the mesh.
 * - [hopCount] tracks how many relay hops away this peer is from the local device.
 * - [rssi] (Received Signal Strength Indicator) is used to gauge proximity for relay selection.
 *
 * @property peerId Unique identifier for this peer (serves as the Room primary key).
 * @property displayName Human-readable name shown in the UI.
 * @property bleAddress The BLE MAC address of the peer, or null if not yet resolved.
 * @property publicKeyBase64 Base64-encoded public key used for encrypted communication.
 * @property discoveredAt Timestamp (ms) when this peer was first seen via BLE scan.
 * @property lastSeenAt Timestamp (ms) of the most recent BLE advertisement or connection.
 * @property rssi Received signal strength in dBm; null when not available.
 * @property state Current connection lifecycle state of this peer.
 * @property isRelay Whether this peer is willing to forward messages for other peers.
 * @property hopCount Number of relay hops between the local device and this peer (0 = direct).
 */
@Entity(tableName = "peers")
data class Peer(
    @PrimaryKey val peerId: String,
    val displayName: String,
    val bleAddress: String? = null,
    val publicKeyBase64: String,
    val discoveredAt: Long = System.currentTimeMillis(),
    val lastSeenAt: Long = System.currentTimeMillis(),
    val rssi: Int? = null,
    val state: PeerState = PeerState.DISCOVERED,
    val isRelay: Boolean = true,
    val hopCount: Int = 0
)

/**
 * Lifecycle states for a [Peer] connection.
 *
 * Models the progression from initial BLE discovery through connection, cryptographic
 * authentication, and eventual disconnection or failure. Consumers should observe state
 * transitions to drive UI indicators and connection management logic.
 */
enum class PeerState {
    /** Peer has been seen via BLE advertisement but no connection attempt has been made. */
    DISCOVERED,

    /** A BLE connection is being established with this peer. */
    CONNECTING,

    /** BLE transport is connected but cryptographic authentication has not yet completed. */
    CONNECTED,

    /** Key exchange handshake is in progress. */
    AUTHENTICATING,

    /** Peer identity has been verified and encrypted communication is ready. */
    AUTHENTICATED,

    /** Connection or authentication failed; the peer may be retried later. */
    FAILED,

    /** An active connection was intentionally closed or lost. */
    DISCONNECTED
}
