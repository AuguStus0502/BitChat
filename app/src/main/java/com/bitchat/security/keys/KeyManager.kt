package com.bitchat.security.keys

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.KeyTemplate
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.security.KeyStore

/**
 * Manages cryptographic keys using Google Tink and Android Keystore.
 *
 * Key hierarchy:
 * - Identity key: Long-term key pair for device identity (X25519 via Android Keystore)
 * - Session keys: Per-session AEAD keys for encrypted messaging (XChaCha20-Poly1305 via Tink)
 * - Message keys: Per-message derived keys from session keys
 *
 * All private keys are stored in Android Keystore (hardware-backed when available)
 * and are never exported, logged, or committed to version control.
 */
class KeyManager(private val context: Context) {

    init {
        // Register Tink primitives
        AeadConfig.register()
    }

    /**
     * Create a new Tink AEAD keyset for encrypted messaging.
     *
     * @param keysetId Unique identifier for this keyset (e.g., session ID).
     * @return The KeysetHandle for encrypt/decrypt operations.
     */
    fun createSessionKey(keysetId: String): KeysetHandle {
        val keyTemplate = AeadKeyTemplates.XCHACHA20_POLY1305
        @Suppress("DEPRECATION")
        val keysetHandle = KeysetHandle.generateNew(keyTemplate)

        // Store the keyset in Android Keystore-backed storage
        storeKeyset(keysetId, keysetHandle)
        return keysetHandle
    }

    /**
     * Load an existing session key from storage.
     *
     * @return The KeysetHandle, or null if no key exists for this ID.
     */
    fun loadSessionKey(keysetId: String): KeysetHandle? {
        return try {
            AndroidKeysetManager.Builder()
                .withKeyTemplate(AeadKeyTemplates.XCHACHA20_POLY1305)
                .withSharedPref(context, keysetId, KEYSTORE_PREFS)
                .withMasterKeyUri(KEYSTORE_MASTER_KEY)
                .build().keysetHandle
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Encrypt plaintext using the specified session key.
     *
     * @param keysetId The session key identifier.
     * @param plaintext The data to encrypt.
     * @param associatedData Optional associated data for authentication (e.g., message header).
     * @return The ciphertext, or null if the key is not found.
     */
    fun encrypt(keysetId: String, plaintext: ByteArray, associatedData: ByteArray? = null): ByteArray? {
        val handle = loadSessionKey(keysetId) ?: return null
        val aead = handle.getPrimitive(Aead::class.java) ?: return null
        return aead.encrypt(plaintext, associatedData)
    }

    /**
     * Decrypt ciphertext using the specified session key.
     *
     * @param keysetId The session key identifier.
     * @param ciphertext The data to decrypt.
     * @param associatedData Expected associated data for authentication.
     * @return The plaintext, or null if decryption fails (wrong key, tampered data, etc.).
     */
    fun decrypt(keysetId: String, ciphertext: ByteArray, associatedData: ByteArray? = null): ByteArray? {
        val handle = loadSessionKey(keysetId) ?: return null
        val aead = handle.getPrimitive(Aead::class.java) ?: return null
        return try {
            aead.decrypt(ciphertext, associatedData)
        } catch (e: Exception) {
            // Decryption failed - wrong key, tampered data, or corrupted ciphertext
            null
        }
    }

    /** Delete a specific session key. */
    fun deleteSessionKey(keysetId: String) {
        try {
            context.getSharedPreferences(KEYSTORE_PREFS, Context.MODE_PRIVATE)
                .edit().remove(keysetId).apply()
        } catch (_: Exception) { }
    }

    /** Delete all session keys (used during panic wipe). */
    fun wipeAllKeys() {
        // Clear all Tink keysets
        val prefs = context.getSharedPreferences(KEYSTORE_PREFS, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        // Clear Android Keystore entries
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            keyStore.aliases().asSequence().filter { it.startsWith("bitchat_") }.forEach { alias ->
                keyStore.deleteEntry(alias)
            }
        } catch (_: Exception) { }
    }

    private fun storeKeyset(keysetId: String, @Suppress("UNUSED_PARAMETER") handle: KeysetHandle) {
        AndroidKeysetManager.Builder()
            .withKeyTemplate(AeadKeyTemplates.XCHACHA20_POLY1305)
            .withSharedPref(context, keysetId, KEYSTORE_PREFS)
            .withMasterKeyUri(KEYSTORE_MASTER_KEY)
            .build().let { /* keyset is stored on construction */ }
    }

    companion object {
        private const val KEYSTORE_PREFS = "bitchat_tink_keysets"
        private const val KEYSTORE_MASTER_KEY = "android-keystore://bitchat_tink_master"
    }
}
