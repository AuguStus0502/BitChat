package com.bitchat.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bitchat.app.BitChatApplication
import com.bitchat.core.models.Identity
import com.bitchat.security.identity.IdentityManager
import com.bitchat.security.keys.KeyManager
import com.bitchat.security.panic.PanicManager
import com.bitchat.storage.repositories.MessageRepository
import com.bitchat.storage.repositories.PeerRepository
import com.bitchat.storage.repositories.QueueRepository
import com.bitchat.storage.repositories.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel powering the [SettingsScreen][com.bitchat.ui.screens.settings.SettingsScreen]
 * with real identity information and panic-wipe operations.
 *
 * ### Responsibilities
 * 1. **Profile display** — exposes the current identity's display name and a truncated
 *    fingerprint (first 12 characters of the Base64-encoded public key) for the
 *    settings UI.
 * 2. **Identity management** — allows updating the display name and rotating the
 *    identity key pair.
 * 3. **Panic wipe** — exposes [PanicManager.WipeState] for driving the emergency wipe
 *    confirmation UI and provides [executePanicWipe] as the destructive action entry point.
 *
 * ### Security Notes
 * - The full public key is never displayed to prevent accidental leakage.
 * - Identity rotation is irreversible — the previous key pair is destroyed.
 * - Panic wipe is the most destructive operation; it clears all data and generates
 *   a fresh identity. The [isWiping] flag prevents concurrent wipe attempts.
 *
 * ### Dependency Wiring
 * All dependencies are constructed from [BitChatApplication.database] without a DI framework.
 * [PanicManager] is constructed with the full dependency graph it requires (identity, keys,
 * messages, peers, sessions, queue repositories).
 *
 * @param application The running [Application] instance, used to access the shared
 *                    [BitChatApplication.database].
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // ── Dependency references ────────────────────────────────────────────

    /** Shared application instance for database access. */
    private val app = application as BitChatApplication

    /** Manages the local cryptographic identity. */
    private val identityManager = IdentityManager(application)

    /** Manages cryptographic keys (for wipe-all during panic). */
    private val keyManager = KeyManager(application)

    /** Repository for messages (required by PanicManager). */
    private val messageRepository = MessageRepository(app.database.messageDao())

    /** Repository for peers (required by PanicManager). */
    private val peerRepository = PeerRepository(app.database.peerDao())

    /** Repository for sessions (required by PanicManager). */
    private val sessionRepository = SessionRepository(app.database.sessionDao())

    /** Repository for the message queue (required by PanicManager). */
    private val queueRepository = QueueRepository(app.database.queueDao())

    /**
     * PanicManager handles full wipe and identity rotation operations.
     * Constructed with all required repositories for complete data destruction.
     */
    private val panicManager = PanicManager(
        context = application,
        identityManager = identityManager,
        keyManager = keyManager,
        messageRepository = messageRepository,
        peerRepository = peerRepository,
        sessionRepository = sessionRepository,
        queueRepository = queueRepository
    )

    // ── Mutable backing fields ───────────────────────────────────────────

    /** The local user's display name. */
    private val _displayName = MutableStateFlow("")

    /**
     * A truncated fingerprint derived from the public key.
     * Format: first 12 characters of the Base64-encoded public key, e.g. "a1B2c3D4e5F6".
     * This provides a visual verification hint without exposing the full key.
     */
    private val _fingerprint = MutableStateFlow("")

    /**
     * The full identity object, populated on construction.
     * Used internally for operations that need the complete identity.
     */
    private var currentIdentity: Identity? = null

    /**
     * True while a panic wipe or identity rotation operation is in progress.
     * Prevents the user from triggering concurrent destructive operations.
     */
    private val _isWiping = MutableStateFlow(false)

    // ── Public read-only StateFlows ──────────────────────────────────────

    /** Observable display name of the local identity. */
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    /** Observable truncated public key fingerprint. */
    val fingerprint: StateFlow<String> = _fingerprint.asStateFlow()

    /** Observable flag indicating a wipe/rotation is in progress. */
    val isWiping: StateFlow<Boolean> = _isWiping.asStateFlow()

    /** Observable wipe state from [PanicManager] for driving progress UI. */
    val wipeState: StateFlow<PanicManager.WipeState> = panicManager.wipeState

    // ── Initialisation ───────────────────────────────────────────────────

    init {
        // Load the current identity and populate display name + fingerprint
        viewModelScope.launch {
            val identity = identityManager.getIdentity()
            currentIdentity = identity
            _displayName.value = identity.displayName
            _fingerprint.value = deriveFingerprint(identity.publicKeyBase64)
        }
    }

    // ── Actions ──────────────────────────────────────────────────────────

    /**
     * Update the local identity's display name.
     *
     * Persists the change through [IdentityManager] which stores it in
     * EncryptedSharedPreferences. The [_displayName] flow is updated
     * immediately for UI reactivity.
     *
     * @param name The new display name. Empty or blank names are rejected.
     */
    fun updateDisplayName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            identityManager.updateDisplayName(trimmed)
            _displayName.value = trimmed
        }
    }

    /**
     * Execute a full panic wipe — destroy all local data and generate a new identity.
     *
     * This is an irreversible operation that:
     * 1. Deletes all messages, peers, sessions, and queue items from the database.
     * 2. Destroys all cryptographic keys from Android Keystore.
     * 3. Wipes the identity and generates a fresh key pair.
     *
     * The [_isWiping] flag is set to true during the operation to prevent concurrent
     * triggers. After completion, [wipeState] transitions to [PanicManager.WipeState.Complete].
     */
    fun executePanicWipe() {
        if (_isWiping.value) return // Prevent concurrent wipe attempts

        viewModelScope.launch {
            _isWiping.value = true
            try {
                panicManager.executeFullWipe()

                // Reload the new identity after wipe
                val newIdentity = identityManager.getIdentity()
                currentIdentity = newIdentity
                _displayName.value = newIdentity.displayName
                _fingerprint.value = deriveFingerprint(newIdentity.publicKeyBase64)
            } catch (e: Exception) {
                // WipeState.Failed is set by PanicManager internally
            } finally {
                _isWiping.value = false
            }
        }
    }

    /**
     * Rotate the device identity without wiping other data.
     *
     * Generates a new key pair and destroys the previous one. Messages, peers,
     * and sessions are preserved, but the device's identity in the mesh changes.
     *
     * This is less destructive than [executePanicWipe] and is useful when the
     * user wants to change their visible identity without losing conversation history.
     */
    fun rotateIdentity() {
        if (_isWiping.value) return

        viewModelScope.launch {
            _isWiping.value = true
            try {
                panicManager.rotateIdentity()

                // Reload the new identity after rotation
                val newIdentity = identityManager.getIdentity()
                currentIdentity = newIdentity
                _displayName.value = newIdentity.displayName
                _fingerprint.value = deriveFingerprint(newIdentity.publicKeyBase64)
            } catch (e: Exception) {
                // WipeState.Failed is set by PanicManager internally
            } finally {
                _isWiping.value = false
            }
        }
    }

    /**
     * Reset the [PanicManager.WipeState] back to [PanicManager.WipeState.Idle].
     *
     * Should be called by the UI after handling the wipe/rotation result
     * (e.g. navigating to the wipe-complete screen or dismissing a dialog).
     */
    fun resetWipeState() {
        panicManager.resetState()
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Derive a short fingerprint from the Base64-encoded public key.
     *
     * Takes the first 12 characters of the Base64 string to produce a
     * human-readable identifier that can be visually compared between peers
     * without exposing the full key material.
     *
     * @param publicKeyBase64 The full Base64-encoded public key.
     * @return A 12-character truncated fingerprint string.
     */
    private fun deriveFingerprint(publicKeyBase64: String): String {
        return if (publicKeyBase64.length >= 12) {
            publicKeyBase64.substring(0, 12)
        } else {
            publicKeyBase64
        }
    }

    // ── Cleanup ──────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        panicManager.destroy()
    }
}
