package com.bitchat.security.identity

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bitchat.core.models.Identity
import com.bitchat.core.utils.IdGenerator
import com.bitchat.storage.repositories.IdentityRepository
import kotlinx.coroutines.flow.first
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore

/**
 * Manages the local cryptographic identity.
 *
 * Generates and stores an X25519 key pair using Android Keystore for private key
 * protection. The public key serves as the device's identity in the BLE protocol.
 * Private keys never leave the Keystore and are not exportable.
 *
 * Identity lifecycle:
 * 1. First launch → generate key pair → create default identity
 * 2. Subsequent launches → load existing identity from storage
 * 3. Display name changes → update in EncryptedSharedPreferences
 * 4. Panic wipe → delete Keystore keys + clear preferences
 */
class IdentityManager(private val context: Context) {

    /** Android Keystore alias for the device's primary identity key pair. */
    private val keyAlias = "bitchat_identity_key"

    /** Encrypted SharedPreferences for non-crypto identity data. */
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "bitchat_identity_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Initialize identity on first launch or return existing identity.
     * Generates a new X25519 key pair if none exists.
     *
     * @return The local identity (with public key Base64-encoded).
     */
    suspend fun initializeOrGetIdentity(): Identity {
        val existing = loadStoredIdentity()
        if (existing != null) return existing

        // Generate new key pair using Android Keystore
        val keyPair = generateKeyPair()
        val publicKeyBase64 = android.util.Base64.encodeToString(
            keyPair.public.encoded,
            android.util.Base64.NO_WRAP
        )

        val identity = Identity(
            identityId = IdGenerator.generateId(),
            displayName = generateDefaultName(),
            publicKeyBase64 = publicKeyBase64
        )

        saveStoredIdentity(identity)
        return identity
    }

    /** Get the current identity, initializing if necessary. */
    suspend fun getIdentity(): Identity = initializeOrGetIdentity()

    /** Update the display name of the current identity. */
    suspend fun updateDisplayName(name: String) {
        val identity = loadStoredIdentity() ?: return
        saveStoredIdentity(identity.copy(displayName = name, lastUsedAt = System.currentTimeMillis()))
    }

    /**
     * Get the Android Keystore key pair for cryptographic operations.
     * Returns null if the key does not exist.
     */
    fun getKeyPair(): KeyPair? {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.PrivateKeyEntry ?: return null
        return KeyPair(entry.certificate.publicKey, entry.privateKey)
    }

    /** Delete all identity data (Keystore key + preferences). */
    suspend fun wipe() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.deleteEntry(keyAlias)
        encryptedPrefs.edit().clear().apply()
    }

    // --- Private helpers ---

    /** Generate an X25519 key pair in Android Keystore. */
    private fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            "AndroidKeyStore"
        )
        generator.initialize(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKeyPair()
    }

    /** Load the stored identity from EncryptedSharedPreferences. */
    private fun loadStoredIdentity(): Identity? {
        val id = encryptedPrefs.getString(KEY_IDENTITY_ID, null) ?: return null
        val name = encryptedPrefs.getString(KEY_DISPLAY_NAME, "") ?: ""
        val pubKey = encryptedPrefs.getString(KEY_PUBLIC_KEY, null) ?: return null
        val created = encryptedPrefs.getLong(KEY_CREATED_AT, 0L)
        return Identity(id, name, pubKey, created)
    }

    /** Persist identity to EncryptedSharedPreferences. */
    private fun saveStoredIdentity(identity: Identity) {
        encryptedPrefs.edit().apply {
            putString(KEY_IDENTITY_ID, identity.identityId)
            putString(KEY_DISPLAY_NAME, identity.displayName)
            putString(KEY_PUBLIC_KEY, identity.publicKeyBase64)
            putLong(KEY_CREATED_AT, identity.createdAt)
            apply()
        }
    }

    /** Generate a friendly default display name. */
    private fun generateDefaultName(): String {
        val adjectives = listOf("Swift", "Bright", "Calm", "Keen", "Warm", "Bold", "Gentle", "Sharp")
        val nouns = listOf("Falcon", "Orion", "Sage", "Coral", "Phoenix", "Breeze", "Stone", "River")
        val adj = adjectives[System.currentTimeMillis().toInt() % adjectives.size]
        val noun = nouns[(System.currentTimeMillis() / 7).toInt() % nouns.size]
        return "$adj $noun"
    }

    companion object {
        private const val KEY_IDENTITY_ID = "identity_id"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_PUBLIC_KEY = "public_key"
        private const val KEY_CREATED_AT = "created_at"
    }
}
