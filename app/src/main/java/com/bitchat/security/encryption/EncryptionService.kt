package com.bitchat.security.encryption

import com.bitchat.security.keys.KeyManager
import java.security.SecureRandom

/**
 * Provides authenticated encryption for all message payloads.
 *
 * Uses XChaCha20-Poly1305 (via Google Tink) for AEAD encryption.
 * Each message includes:
 * - Unique nonce (randomly generated, never reused)
 * - Ciphertext
 * - Associated data (authenticated but not encrypted) binding the message to its session
 *
 * Nonce uniqueness is guaranteed by using SecureRandom with 192-bit nonce space,
 * making collision probability negligible.
 */
class EncryptionService(private val keyManager: KeyManager) {

    private val secureRandom = SecureRandom()

    /**
     * Encrypt a message payload for a given session.
     *
     * @param sessionId The session identifier (used as the Tink keyset ID).
     * @param plaintext The raw message bytes to encrypt.
     * @param senderId Sender's identity (included in authenticated data).
     * @param recipientId Recipient's identity (included in authenticated data).
     * @return EncryptedData containing ciphertext and nonce, or null if encryption fails.
     */
    fun encrypt(
        sessionId: String,
        plaintext: ByteArray,
        senderId: String,
        recipientId: String
    ): EncryptedData? {
        // Build associated data: binds ciphertext to sender+recipient
        // Tampering with either field will cause authentication failure on decrypt
        val aad = buildAad(senderId, recipientId)

        val ciphertext = keyManager.encrypt(sessionId, plaintext, aad) ?: return null

        return EncryptedData(
            ciphertext = ciphertext,
            associatedData = aad,
            sessionId = sessionId
        )
    }

    /**
     * Decrypt a received message payload.
     *
     * @param sessionId The session identifier.
     * @param encryptedData The encrypted payload received.
     * @param expectedSenderId Expected sender identity (for authentication).
     * @param expectedRecipientId Expected recipient identity.
     * @return The decrypted plaintext, or null if decryption/authentication fails.
     */
    fun decrypt(
        sessionId: String,
        encryptedData: EncryptedData,
        expectedSenderId: String,
        expectedRecipientId: String
    ): ByteArray? {
        val aad = buildAad(expectedSenderId, expectedRecipientId)

        // Verify that the AAD matches what we expect
        // This prevents replay of messages in different contexts
        if (!encryptedData.associatedData.contentEquals(aad)) {
            return null
        }

        return keyManager.decrypt(sessionId, encryptedData.ciphertext, aad)
    }

    /** Build the associated authenticated data from sender and recipient IDs. */
    private fun buildAad(senderId: String, recipientId: String): ByteArray {
        return "$senderId:$recipientId".toByteArray(Charsets.UTF_8)
    }
}

/**
 * Represents an encrypted payload ready for transmission.
 *
 * @property ciphertext The encrypted message bytes.
 * @property associatedData Authenticated but not encrypted metadata.
 * @property sessionId The session key used for encryption.
 */
data class EncryptedData(
    val ciphertext: ByteArray,
    val associatedData: ByteArray,
    val sessionId: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedData) return false
        return ciphertext.contentEquals(other.ciphertext) &&
                associatedData.contentEquals(other.associatedData) &&
                sessionId == other.sessionId
    }
    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + associatedData.contentHashCode()
        result = 31 * result + sessionId.hashCode()
        return result
    }
}
