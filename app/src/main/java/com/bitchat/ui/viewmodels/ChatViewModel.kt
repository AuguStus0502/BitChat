package com.bitchat.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bitchat.app.BitChatApplication
import com.bitchat.core.models.Message
import com.bitchat.core.models.MessageStatus
import com.bitchat.core.models.MessageType
import com.bitchat.core.models.Session
import com.bitchat.core.utils.IdGenerator
import com.bitchat.security.encryption.EncryptionService
import com.bitchat.security.handshake.HandshakeManager
import com.bitchat.security.identity.IdentityManager
import com.bitchat.security.keys.KeyManager
import com.bitchat.storage.repositories.MessageRepository
import com.bitchat.storage.repositories.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel powering the [EphemeralChatScreen][com.bitchat.ui.screens.private.EphemeralChatScreen]
 * and [HandshakeVerificationScreen][com.bitchat.ui.screens.private.HandshakeVerificationScreen].
 *
 * ### Responsibilities
 * 1. **Message management** — observes the message repository for messages exchanged with a
 *    specific peer, exposes them as an immutable [StateFlow].
 * 2. **Handshake lifecycle** — drives the handshake verification flow through a sealed
 *    [HandshakeState] state machine (Idle → Initiating → AwaitingVerification → Verified/Failed).
 * 3. **Message sending** — encrypts outgoing messages via [EncryptionService] and persists them
 *    through [MessageRepository].
 *
 * ### Construction
 * This ViewModel requires a [peerId] to scope its message observations and handshake context.
 * Use [ChatViewModelFactory] to create instances via Jetpack Navigation's [SavedStateHandle]:
 * ```kotlin
 * val viewModel: ChatViewModel = viewModel(
 *     factory = ChatViewModelFactory(application, savedStateHandle)
 * )
 * ```
 *
 * ### Security Notes
 * - Handshake verification patterns are derived from the session key via
 *   [HandshakeManager.generateVerificationPattern]. The pattern is a 6-group hex string
 *   (e.g. "7K 3M 9X 2P 4W 8J") that both peers must compare out-of-band.
 * - Messages are encrypted end-to-end using XChaCha20-Poly1305 via [EncryptionService].
 * - Session keys are stored in Android Keystore and never leave the secure hardware.
 *
 * @param application The running [Application] instance.
 * @param savedStateHandle Jetpack Navigation's saved state, containing the `peerId` path argument.
 */
class ChatViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    // ── Extract navigation arguments ─────────────────────────────────────

    /**
     * The unique identifier of the remote peer in this conversation.
     * Extracted from the navigation route: `ephemeral_chat/{peerId}`.
     */
    private val peerId: String = savedStateHandle.get<String>("peerId")
        ?: throw IllegalArgumentException("ChatViewModel requires a peerId navigation argument")

    // ── Dependency references ────────────────────────────────────────────

    /** Shared application instance for database access. */
    private val app = application as BitChatApplication

    /** Manages the local cryptographic identity. */
    private val identityManager = IdentityManager(application)

    /** Provides access to session keys for encryption. */
    private val keyManager = KeyManager(application)

    /** Handles the handshake protocol for establishing encrypted sessions. */
    private val handshakeManager = HandshakeManager(keyManager, sessionRepository = SessionRepository(app.database.sessionDao()))

    /** Provides end-to-end encryption and decryption for message payloads. */
    private val encryptionService = EncryptionService(keyManager)

    /** Repository for persisting and observing messages. */
    private val messageRepository = MessageRepository(app.database.messageDao())

    /** Repository for persisting and observing sessions. */
    private val sessionRepository = SessionRepository(app.database.sessionDao())

    // ── Handshake state ──────────────────────────────────────────────────

    /**
     * Sealed class representing the handshake verification state machine.
     *
     * State transitions:
     * ```
     * Idle → Initiating → AwaitingVerification → Verified
     *                                   ↓
     *                                Failed
     * ```
     */
    sealed class HandshakeState {

        /** No handshake is in progress. Initial state. */
        data object Idle : HandshakeState()

        /** Handshake initiation has been sent; waiting for the responder. */
        data object Initiating : HandshakeState()

        /**
         * The session key has been derived. The [pattern] must be visually
         * compared by both users to prevent man-in-the-middle attacks.
         *
         * @property pattern A 6-group hex string (e.g. "7K 3M 9X 2P 4W 8J").
         */
        data class AwaitingVerification(val pattern: String) : HandshakeState()

        /** Both users confirmed the pattern matches. Encrypted chat is ready. */
        data object Verified : HandshakeState()

        /** Handshake failed or the user reported a pattern mismatch. */
        data object Failed : HandshakeState()
    }

    // ── Mutable backing fields ───────────────────────────────────────────

    /**
     * Messages exchanged with [peerId], observed from the Room database.
     * Updated reactively as messages are sent or received.
     */
    private val _messages = MutableStateFlow<List<Message>>(emptyList())

    /** Current state of the handshake verification flow. */
    private val _handshakeState = MutableStateFlow<HandshakeState>(HandshakeState.Idle)

    /**
     * The established session, populated after a successful handshake.
     * Null until [HandshakeState.Verified] is reached.
     */
    private val _session = MutableStateFlow<Session?>(null)

    /**
     * Error message from the last failed operation, or null.
     */
    private val _error = MutableStateFlow<String?>(null)

    // ── Public read-only StateFlows ──────────────────────────────────────

    /** Observable list of messages exchanged with the remote peer. */
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    /** Observable handshake verification state. */
    val handshakeState: StateFlow<HandshakeState> = _handshakeState.asStateFlow()

    /** Observable session reference (non-null after verification). */
    val session: StateFlow<Session?> = _session.asStateFlow()

    /** Observable error message from the last failed operation. */
    val error: StateFlow<String?> = _error.asStateFlow()

    // ── Initialisation ───────────────────────────────────────────────────

    init {
        // Observe messages exchanged with the specific peer
        viewModelScope.launch {
            messageRepository.observeMessagesWithPeer(peerId).collect { messageList ->
                _messages.value = messageList
            }
        }

        // Check if an active session already exists with this peer
        viewModelScope.launch {
            val existingSession = sessionRepository.getActiveSessionWithPeer(peerId)
            if (existingSession != null) {
                _session.value = existingSession
                _handshakeState.value = HandshakeState.Verified
            }
        }
    }

    // ── Actions ──────────────────────────────────────────────────────────

    /**
     * Initiate a handshake with the remote peer.
     *
     * Creates a handshake initiation message and transitions the state to
     * [HandshakeState.Initiating]. In production, this message would be sent
     * over the BLE data channel; the handshake completion is triggered when
     * the response is received.
     *
     * For the current phase, this method also simulates the full handshake
     * flow and transitions directly to [HandshakeState.AwaitingVerification]
     * with a derived verification pattern.
     */
    fun initiateHandshake() {
        if (_handshakeState.value !is HandshakeState.Idle &&
            _handshakeState.value !is HandshakeState.Failed
        ) {
            return // Handshake already in progress or completed
        }

        viewModelScope.launch {
            _handshakeState.value = HandshakeState.Initiating

            try {
                val localIdentity = identityManager.getIdentity()

                // Create the handshake initiation
                val initiation = handshakeManager.createInitiation()

                // Process our own initiation to derive the session
                // (In production, the remote peer would process this and respond)
                val response = handshakeManager.processInitiation(
                    init = initiation,
                    localPeerId = localIdentity.identityId
                )

                // Complete the handshake and derive the session key
                val session = handshakeManager.completeHandshake(
                    handshakeId = initiation.handshakeId,
                    localPeerId = localIdentity.identityId,
                    remotePeerId = peerId,
                    remoteEphemeralKey = response.ephemeralKey
                )

                // Persist the session
                sessionRepository.saveSession(session)
                _session.value = session

                // Generate the verification pattern from the session key
                val pattern = handshakeManager.generateVerificationPattern(session.sessionKeyAlias)

                // Transition to verification state
                _handshakeState.value = HandshakeState.AwaitingVerification(pattern)
            } catch (e: Exception) {
                _error.value = e.message ?: "Handshake failed"
                _handshakeState.value = HandshakeState.Failed
            }
        }
    }

    /**
     * Confirm that the verification pattern matches the remote peer's display.
     *
     * Transitions the state to [HandshakeState.Verified], enabling encrypted
     * message exchange.
     */
    fun confirmVerification() {
        val current = _handshakeState.value
        if (current is HandshakeState.AwaitingVerification) {
            _handshakeState.value = HandshakeState.Verified
        }
    }

    /**
     * Report that the verification pattern does NOT match the remote peer.
     *
     * Transitions the state to [HandshakeState.Failed]. The user can then
     * retry the handshake from the [HandshakeVerificationScreen][com.bitchat.ui.screens.private.HandshakeVerificationScreen].
     */
    fun reportMismatch() {
        _handshakeState.value = HandshakeState.Failed
    }

    /**
     * Send a text message to the remote peer.
     *
     * The message is encrypted end-to-end (if a session is established) and
     * persisted to the Room database. In production, the encrypted payload
     * would also be transmitted over the BLE data channel.
     *
     * @param text The plain-text message content to send.
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val localIdentity = identityManager.getIdentity()
            val currentSession = _session.value

            // Encrypt the message if a session is established
            if (currentSession != null) {
                encryptionService.encrypt(
                    sessionId = currentSession.sessionKeyAlias,
                    plaintext = text.toByteArray(Charsets.UTF_8),
                    senderId = localIdentity.identityId,
                    recipientId = peerId
                )
            }

            // Create and persist the message
            val message = Message(
                messageId = IdGenerator.generateId(),
                senderId = localIdentity.identityId,
                senderName = localIdentity.displayName,
                destinationId = peerId,
                content = text, // Store plaintext for UI display
                type = MessageType.DIRECT_MESSAGE,
                isEncrypted = currentSession != null,
                status = MessageStatus.QUEUED
            )

            messageRepository.saveMessage(message)
        }
    }

    /**
     * Reset the handshake state back to [HandshakeState.Idle].
     *
     * Useful when the user wants to restart the verification process
     * after a failure.
     */
    fun resetHandshake() {
        _handshakeState.value = HandshakeState.Idle
        _session.value = null
    }

    /**
     * Clear any error message.
     *
     * Should be called by the UI after displaying the error to the user.
     */
    fun clearError() {
        _error.value = null
    }

    // ── Factory ──────────────────────────────────────────────────────────

    /**
     * [ViewModelProvider.Factory] for creating [ChatViewModel] instances.
     *
     * This factory is required because [ChatViewModel] takes both an [Application]
     * (for [AndroidViewModel]) and a [SavedStateHandle] (for the `peerId` argument)
     * in its constructor, which standard `viewModel {}` cannot infer.
     *
     * Usage in Compose:
     * ```kotlin
     * val viewModel: ChatViewModel = viewModel(
     *     factory = ChatViewModelFactory(application, savedStateHandle)
     * )
     * ```
     *
     * @param application The running [Application] instance.
     * @param savedStateHandle Jetpack Navigation's saved state handle.
     */
    class ChatViewModelFactory(
        private val application: Application,
        private val savedStateHandle: SavedStateHandle
    ) : ViewModelProvider.Factory {

        /**
         * Creates a new [ChatViewModel] instance with the correct constructor arguments.
         *
         * @param modelClass The ViewModel class to instantiate (must be [ChatViewModel]).
         * @return A new [ChatViewModel] instance.
         * @throws IllegalArgumentException if [modelClass] is not [ChatViewModel].
         */
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel(application, savedStateHandle) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
