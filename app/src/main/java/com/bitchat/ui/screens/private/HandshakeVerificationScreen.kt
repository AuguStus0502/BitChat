package com.bitchat.ui.screens.private

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bitchat.ui.navigation.Screen
import com.bitchat.ui.theme.*
import com.bitchat.ui.viewmodels.ChatViewModel

/**
 * Handshake Verification screen — the second step in BitChat's private-mode flow.
 *
 * After a peer is discovered ([PeerDiscoveryScreen]) or selected via a contact
 * token ([ContactTokenScreen]), this screen performs a **visual fingerprint
 * comparison** to let both users mutually verify that they are connected to the
 * intended device without a man-in-the-middle.
 *
 * ### Verification States
 *
 * The screen progresses through a four-state machine:
 *
 * | State | Description |
 * |---|---|
 * | [VerificationState.VERIFYING] | Initial state. An indeterminate progress spinner is shown while the encrypted session is being established with the peer over BLE. |
 * | [VerificationState.PATTERN_DISPLAY] | The session fingerprint is displayed. A pulsing animation on the verification icon draws attention to the pattern. Both users must visually compare the code shown on their devices. |
 * | [VerificationState.MISMATCH] | The user tapped "Mismatch" — the patterns do not match. A warning is shown with a potential man-in-the-middle alert, and the user can retry. |
 * | [VerificationState.VERIFIED] | The user tapped "Confirm Match" — the patterns match. Navigation proceeds to [EphemeralChatScreen] and the handshake screen is popped from the back stack. |
 *
 * ### Animation & Haptic Notes
 *
 * - The verification icon uses a **continuous scale pulse** (1.0 → 1.1 → 1.0)
 *   driven by an [Animatable] with 500 ms [tween] easing to draw the user's
 *   eye to the fingerprint pattern during comparison.
 * - Haptic feedback (e.g. vibration on state transitions) is planned for a
 *   later phase and is not yet wired.
 *
 * **Phase note:** The session fingerprint (`"7K 3M 9X 2P 4W 8J"`) is currently
 * a static placeholder. In production it will be derived from the ephemeral
 * Diffie-Hellman key exchange between the two peers.
 *
 * @param peerId   The unique identifier of the peer being verified, passed from
 *                 [PeerDiscoveryScreen] or [ContactTokenScreen].
 * @param navController Navigation controller used to navigate to the chat screen
 *                      on successful verification or to go back on failure.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandshakeVerificationScreen(peerId: String, navController: NavController) {
    val chatViewModel: ChatViewModel = viewModel()
    val handshakeState by chatViewModel.handshakeState.collectAsStateWithLifecycle()

    val verificationState = when (handshakeState) {
        is ChatViewModel.HandshakeState.Idle,
        is ChatViewModel.HandshakeState.Initiating -> VerificationState.VERIFYING
        is ChatViewModel.HandshakeState.AwaitingVerification -> VerificationState.PATTERN_DISPLAY
        is ChatViewModel.HandshakeState.Verified -> VerificationState.VERIFIED
        is ChatViewModel.HandshakeState.Failed -> VerificationState.MISMATCH
    }
    val verificationPattern = (handshakeState as? ChatViewModel.HandshakeState.AwaitingVerification)?.pattern ?: ""

    // Reusable animatable for the pulsing scale effect on the verification icon.
    val scale = remember { Animatable(1f) }

    // Initiate handshake on first composition.
    LaunchedEffect(Unit) {
        chatViewModel.initiateHandshake()
    }

    // Pulsing scale animation — runs only while in PATTERN_DISPLAY state.
    // The icon scales up to 1.1× and back to 1.0× in a continuous loop,
    // using a symmetric 500 ms ease-in-out tween for a smooth breathing effect.
    LaunchedEffect(verificationState) {
        if (verificationState == VerificationState.PATTERN_DISPLAY) {
            while (true) {
                scale.animateTo(1.1f, tween(500))
                scale.animateTo(1f, tween(500))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secure Handshake") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Render UI based on the current verification state.
            when (verificationState) {

                // ---------------------------------------------------------
                // VERIFYING — waiting for the encrypted session to establish.
                // ---------------------------------------------------------
                VerificationState.VERIFYING -> {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp))
                    Text("Verifying connection...", fontSize = 16.sp)
                    Text(
                        "Establishing encrypted session with peer",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ---------------------------------------------------------
                // PATTERN_DISPLAY — show the fingerprint for visual comparison.
                // ---------------------------------------------------------
                VerificationState.PATTERN_DISPLAY -> {
                    // Pulsing verification icon — the scale is driven by the
                    // LaunchedEffect loop defined above.
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(scale.value),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = null,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    }
                    Text("Verification Pattern", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "Compare this pattern with your peer. Both devices should show the same pattern.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Fingerprint card — the session-specific code both users must compare.
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Session Fingerprint", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text(
                                verificationPattern,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // User decision buttons — "Mismatch" or "Confirm Match".
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { chatViewModel.reportMismatch() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mismatch")
                        }
                        Button(
                            onClick = {
                                chatViewModel.confirmVerification()
                                navController.navigate(Screen.EphemeralChat.createRoute(peerId)) {
                                    popUpTo(Screen.HandshakeVerification.createRoute(peerId)) { inclusive = true }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = EmergencyStable)
                        ) {
                            Text("Confirm Match")
                        }
                    }
                }

                // ---------------------------------------------------------
                // MISMATCH — patterns do not match; warn about potential MITM.
                // ---------------------------------------------------------
                VerificationState.MISMATCH -> {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = EmergencyCritical
                    )
                    Text("Verification Failed", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EmergencyCritical)
                    Text(
                        "The patterns do not match. This could indicate a man-in-the-middle attack.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Retry — return to pattern display so the user can re-compare.
                    Button(
                        onClick = {
                            chatViewModel.resetHandshake()
                            chatViewModel.initiateHandshake()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Retry")
                    }
                }

                // ---------------------------------------------------------
                // VERIFIED — successful match; navigate away.
                // ---------------------------------------------------------
                VerificationState.VERIFIED -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = EmergencyStable
                    )
                    Text("Verified!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}

/**
 * Represents the four possible states of the handshake verification flow.
 *
 * - [VERIFYING]       — initial connection setup in progress.
 * - [PATTERN_DISPLAY] — fingerprint is shown for visual comparison.
 * - [MISMATCH]        — user reported a mismatch; potential MITM.
 * - [VERIFIED]        — user confirmed the patterns match; proceeding to chat.
 */
private enum class VerificationState {
    VERIFYING,
    PATTERN_DISPLAY,
    MISMATCH,
    VERIFIED
}
