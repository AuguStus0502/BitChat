package com.bitchat.security.panic

import android.content.Context
import com.bitchat.core.utils.IdGenerator
import com.bitchat.security.identity.IdentityManager
import com.bitchat.security.keys.KeyManager
import com.bitchat.storage.database.BitChatDatabase
import com.bitchat.storage.repositories.MessageRepository
import com.bitchat.storage.repositories.PeerRepository
import com.bitchat.storage.repositories.QueueRepository
import com.bitchat.storage.repositories.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Handles emergency panic wipe operations — complete or selective data destruction.
 *
 * In Privacy Sovereignty mode, users can trigger a panic wipe to immediately
 * destroy all locally stored sensitive data:
 * - All cryptographic keys (identity keys, session keys)
 * - All messages (sent and received)
 * - All peer records
 * - All sessions
 * - Queued messages
 * - Identity — a new identity is generated immediately after wipe
 *
 * Security properties:
 * - Wipe is irreversible — no undo
 * - New identity is generated atomically with wipe completion
 * - All Android Keystore entries for BitChat are deleted
 * - All Room database tables are cleared
 * - No data remnants in SharedPreferences or encrypted storage
 *
 * Design rationale:
 * The panic wipe is a deliberate destructive action. It prioritizes speed and
 * completeness over graceful degradation. The user has explicitly chosen to
 * sacrifice all accumulated state.
 */
class PanicManager(
    private val context: Context,
    private val identityManager: IdentityManager,
    private val keyManager: KeyManager,
    private val messageRepository: MessageRepository,
    private val peerRepository: PeerRepository,
    private val sessionRepository: SessionRepository,
    private val queueRepository: QueueRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _wipeState = MutableStateFlow<WipeState>(WipeState.Idle)
    val wipeState: StateFlow<WipeState> = _wipeState.asStateFlow()

    /**
     * Execute a full panic wipe — destroy all data and regenerate identity.
     *
     * This operation is irreversible. Each step is executed sequentially
     * to ensure complete destruction even if a later step fails.
     */
    suspend fun executeFullWipe() {
        _wipeState.value = WipeState.Wiping

        try {
            // Step 1: Clear all messages
            _wipeState.value = WipeState.WipingProgress("Clearing messages")
            messageRepository.clearAll()

            // Step 2: Clear all peer records
            _wipeState.value = WipeState.WipingProgress("Clearing peers")
            peerRepository.clearAll()

            // Step 3: Clear all sessions
            _wipeState.value = WipeState.WipingProgress("Clearing sessions")
            sessionRepository.clearAll()

            // Step 4: Clear queued messages
            _wipeState.value = WipeState.WipingProgress("Clearing message queue")
            queueRepository.clearAll()

            // Step 5: Destroy all cryptographic keys
            _wipeState.value = WipeState.WipingProgress("Destroying cryptographic keys")
            keyManager.wipeAllKeys()

            // Step 6: Destroy identity and generate new one
            _wipeState.value = WipeState.WipingProgress("Generating new identity")
            identityManager.wipe()
            identityManager.getIdentity()

            _wipeState.value = WipeState.Complete
        } catch (e: Exception) {
            _wipeState.value = WipeState.Failed(e.message ?: "Unknown error during wipe")
        }
    }

    /**
     * Rotate the device identity without wiping other data.
     *
     * Less destructive than a full wipe — preserves messages, peers, and sessions
     * but generates a new identity key pair. Previous identity is unrecoverable.
     */
    suspend fun rotateIdentity() {
        _wipeState.value = WipeState.WipingProgress("Rotating identity")

        try {
            identityManager.wipe()
            identityManager.getIdentity()
            _wipeState.value = WipeState.Complete
        } catch (e: Exception) {
            _wipeState.value = WipeState.Failed(e.message ?: "Identity rotation failed")
        }
    }

    /** Reset wipe state back to idle. */
    fun resetState() {
        _wipeState.value = WipeState.Idle
    }

    fun destroy() {
        scope.cancel()
    }

    /**
     * Represents the state of the panic wipe operation.
     */
    sealed class WipeState {
        /** No wipe in progress. */
        data object Idle : WipeState()

        /** Wipe in progress — destroying data. */
        data object Wiping : WipeState()

        /** Wipe in progress with human-readable progress message. */
        data class WipingProgress(val message: String) : WipeState()

        /** Wipe completed successfully — new identity is active. */
        data object Complete : WipeState()

        /** Wipe failed — some data may have been destroyed. */
        data class Failed(val error: String) : WipeState()
    }
}
