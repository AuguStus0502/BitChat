package com.bitchat.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A local user identity used for cryptographic signing and authentication on the BitChat network.
 *
 * Each identity holds a keypair (public portion stored here, private key in Android Keystore)
 * and a human-readable display name. Users may maintain multiple identities for privacy
 * separation (e.g., work vs. personal). Exactly one identity is marked as [isDefault] and
 * is automatically selected when composing new messages.
 *
 * Identities are persisted in a Room database so they survive app restarts. The [lastUsedAt]
 * field is updated on every send to support "most recently used" selection logic.
 *
 * @property identityId Unique identifier for this identity (also the Room primary key).
 * @property displayName Human-readable name displayed to remote peers.
 * @property publicKeyBase64 Base64-encoded public key shared with other peers during handshake.
 * @property createdAt Timestamp (ms) when this identity was first created.
 * @property isDefault Whether this identity is the default for composing new messages.
 * @property lastUsedAt Timestamp (ms) of the most recent use of this identity for sending.
 */
@Entity(tableName = "identities")
data class Identity(
    @PrimaryKey val identityId: String,
    val displayName: String,
    val publicKeyBase64: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isDefault: Boolean = true,
    val lastUsedAt: Long = System.currentTimeMillis()
)
