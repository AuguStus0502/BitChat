package com.bitchat.ui.screens.private

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bitchat.ui.theme.*
import com.bitchat.ui.viewmodels.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A single message within an ephemeral chat session.
 *
 * @property text        The plain-text content of the message.
 * @property isMine      `true` if this message was sent by the local user,
 *                       `false` if it was received from the remote peer.
 * @property timestamp   Human-readable time label (e.g. "10:30" or "Now").
 * @property isDelivered `true` once the message has been acknowledged by the
 *                       remote peer. Drives the single/double-check delivery
 *                       indicator in the bubble.
 */
data class ChatMessage(
    val text: String,
    val isMine: Boolean,
    val timestamp: String,
    val isDelivered: Boolean = false
)

/**
 * Ephemeral Chat screen — the final step in BitChat's private-mode flow.
 *
 * After a successful handshake ([HandshakeVerificationScreen]), the two peers
 * enter this screen for **end-to-end encrypted, session-scoped messaging**.
 *
 * ### Privacy Properties
 *
 * - **Ephemeral** — all messages exist only in memory for the duration of the
 *   session. Closing the session discards the conversation.
 * - **End-to-end encrypted** — messages are encrypted on the sender's device
 *   and decrypted only on the recipient's device. The server (if any) never
 *   sees plaintext.
 * - **Session-scoped** — each session has a unique encryption context derived
 *   from the handshake key exchange; messages cannot be replayed into a
 *   different session.
 *
 * ### Current Limitations (Phase Note)
 *
 * - Messages are currently stored in local Compose state with placeholder data.
 *   In a later phase this will be backed by an encrypted in-memory message
 *   queue and the BLE data channel.
 * - Delivery receipts (`isDelivered`) are not yet wired to real acknowledgments.
 * - The session info button (lock icon in the top bar) is a placeholder and
 *   will display session metadata (peer ID, encryption parameters, uptime) in
 *   a future iteration.
 *
 * @param peerId         The unique identifier of the connected peer (from the handshake).
 * @param navController  Navigation controller; navigating back exits the session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EphemeralChatScreen(@Suppress("UNUSED_PARAMETER") peerId: String, navController: NavController) {
    val chatViewModel: ChatViewModel = viewModel()
    val rawMessages by chatViewModel.messages.collectAsStateWithLifecycle()

    // Current text in the compose input field.
    var inputText by remember { mutableStateOf("") }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val localId = remember { "" } // Will be empty until identity loads
    val messages = remember(rawMessages) {
        rawMessages.map { msg ->
            ChatMessage(
                text = msg.content,
                isMine = msg.senderId == localId,
                timestamp = timeFormat.format(Date(msg.timestamp)),
                isDelivered = msg.status.name == "DELIVERED"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Encrypted Session", fontSize = 16.sp)
                        // Subtitle reinforces the ephemeral + secure nature of the session.
                        Text(
                            text = "Ephemeral | Secure",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    // Back action — exits the session.
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Session info button (placeholder) — will display encryption
                    // parameters, peer identity, and session uptime in a future phase.
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Lock, contentDescription = "Session info")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Ephemeral session banner — persistent reminder that messages are
            // encrypted and will not be stored after the session ends.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = EmergencyStableBg
            ) {
                Text(
                    text = "Messages are end-to-end encrypted and ephemeral.",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = EmergencyStable
                )
            }

            // Message list — newest messages at the bottom (natural chat order).
            // Each item is rendered as a [ChatBubble].
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
            }

            // Compose bar — text input and send button.
            Surface(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type a message...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )
                    // Send button — sends message via ViewModel.
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                chatViewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders a single chat bubble for a [ChatMessage].
 *
 * - **Sent messages** (`isMine = true`) are aligned to the **end** (right) and
 *   use the primary theme color with white text.
 * - **Received messages** (`isMine = false`) are aligned to the **start** (left)
 *   and use the surface-variant background.
 *
 * The bubble shape uses **asymmetric rounded corners**: the sender's side is
 * more rounded while the receiver's side has a smaller radius, creating the
 * classic messaging-app silhouette.
 *
 * A delivery indicator is shown for sent messages:
 * - Single check ([Icons.Default.Done]) — message sent.
 * - Double check ([Icons.Default.DoneAll]) — message delivered to peer.
 *
 * @param message The [ChatMessage] data model to render.
 */
@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 280.dp),
            // Asymmetric corners: the "tail" corner is smaller to indicate direction.
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isMine) 16.dp else 4.dp,
                bottomEnd = if (message.isMine) 4.dp else 16.dp
            ),
            color = if (message.isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = if (message.isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Timestamp and optional delivery indicator row.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timestamp,
                        fontSize = 10.sp,
                        color = if (message.isMine) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Delivery check marks — only shown for sent messages.
                    if (message.isMine) {
                        Icon(
                            if (message.isDelivered) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
