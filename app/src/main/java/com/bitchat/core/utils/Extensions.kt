package com.bitchat.core.utils

import java.security.SecureRandom
import java.util.UUID

/**
 * Thread-safe generator for unique identifiers used throughout the BitChat application.
 *
 * Provides two flavors of ID generation:
 * - [generateId]: 32-character hex string derived from a random UUID (no dashes).
 * - [generateShortId]: 16-character hex string derived from 8 cryptographically random bytes.
 *
 * Both methods use [SecureRandom] to ensure unpredictability, which is important for
 * peer IDs, message IDs, and session identifiers that may be visible on the mesh network.
 */
object IdGenerator {

    /** Cryptographically secure random number generator shared by all generation methods. */
    private val secureRandom = SecureRandom()

    /**
     * Generates a 32-character uppercase hex identifier from a random UUID.
     *
     * The output is a UUID v4 with hyphens removed, yielding 128 bits of randomness
     * encoded as 32 hex characters (e.g., "a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6").
     *
     * @return A unique 32-character hex string.
     */
    fun generateId(): String = UUID.randomUUID().toString().replace("-", "")

    /**
     * Generates a compact 16-character hex identifier from 8 random bytes.
     *
     * Uses [SecureRandom] to produce 8 bytes (64 bits of entropy) and encodes them
     * as a 16-character lowercase hex string. Suitable for short-lived identifiers
     * such as ephemeral queue keys or temporary session tokens.
     *
     * @return A unique 16-character hex string.
     */
    fun generateShortId(): String {
        val bytes = ByteArray(8)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
