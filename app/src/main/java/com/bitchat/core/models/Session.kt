package com.bitchat.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an encrypted communication session between the local device and a remote peer.
 *
 * Sessions are established after a successful cryptographic handshake and hold a reference
 * to the negotiated session key stored in the Android Keystore ([sessionKeyAlias]). Once
 * established, all [Message] payloads exchanged between the two peers are encrypted using
 * this session key.
 *
 * Lifecycle:
 * 1. Created when a HANDSHAKE message completes successfully.
 * 2. Remains [isActive] as long as the BLE connection is alive or the session has not expired.
 * 3. Deactivated when the peer disconnects, the session expires, or the user explicitly closes it.
 * 4. [isEphemeral] sessions are automatically purged on app restart; persistent sessions survive.
 *
 * @property sessionId Unique identifier for this session (Room primary key).
 * @property localPeerId The peer ID of the local device.
 * @property remotePeerId The peer ID of the remote device.
 * @property sessionKeyAlias Android Keystore alias for the AES session key used to encrypt payloads.
 * @property createdAt Timestamp (ms) when the session was first established.
 * @property lastActivityAt Timestamp (ms) of the most recent encrypted message exchange.
 * @property expiresAt Absolute timestamp (ms) after which the session is automatically closed; null for indefinite sessions.
 * @property isActive Whether the session is currently open and ready for encrypted communication.
 * @property isEphemeral Whether this session is discarded on app restart (true) or persisted (false).
 */
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val sessionId: String,
    val localPeerId: String,
    val remotePeerId: String,
    val sessionKeyAlias: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActivityAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val isActive: Boolean = true,
    val isEphemeral: Boolean = true
)
