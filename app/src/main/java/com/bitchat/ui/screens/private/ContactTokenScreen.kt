package com.bitchat.ui.screens.private

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bitchat.ui.navigation.Screen
import com.bitchat.ui.theme.*

/**
 * Contact Token screen — an alternative entry point to the private-mode flow.
 *
 * Instead of discovering peers via BLE, this screen lets two users who are
 * physically close exchange short human-readable tokens (e.g. read aloud or
 * displayed on-screen) to bootstrap a private session.
 *
 * ### Token Security Model
 *
 * - A token is a **temporary, random, human-readable identifier** (format `BC-XXXX-XXXX-XXXX`).
 * - Tokens are used for **local peer identification only** — they help two devices
 *   agree on who to handshake with, but do **not** provide cryptographic security by
 *   themselves.
 * - Actual encryption and authentication are established during the subsequent
 *   [HandshakeVerificationScreen] step via fingerprint comparison.
 * - Tokens should be treated as **ephemeral** — they are regenerated periodically
 *   and must not be stored or shared beyond the immediate pairing context.
 *
 * ### Limitations
 *
 * - Token exchange is **out-of-band** (voice, QR code, etc.) and relies on the
 *   user to correctly transcribe the token.
 * - A token alone cannot prevent impersonation; the visual handshake fingerprint
 *   is the true verification mechanism.
 * - In a future phase, token generation and validation will be backed by the
 *   cryptographic identity layer.
 *
 * **Phase note:** Token input currently stores the value in local UI state. The
 * "Connect" action will be wired to the peer resolution and handshake service
 * in a later phase.
 *
 * @param navController Navigation controller for transitioning between private-mode screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactTokenScreen(navController: NavController) {
    // User-entered peer token to connect with.
    var tokenInput by remember { mutableStateOf("") }

    // This device's current contact token.
    // TODO: Generate dynamically from the identity service; do not hard-code.
    var myToken by remember { mutableStateOf("BC-7K3M-9X2P-4W8J") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Token") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ---------------------------------------------------------------
            // "Your Token" card — displays this device's token for sharing.
            // ---------------------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Your Token", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    // Large, spaced token string for easy reading aloud or copying.
                    Text(
                        text = myToken,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Share this token with someone you want to connect with privately.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            HorizontalDivider()

            Text("Connect with a peer", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

            // Peer token input — user types the token they received from another device.
            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                label = { Text("Enter peer token") },
                placeholder = { Text("BC-XXXX-XXXX-XXXX") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Token, contentDescription = null) }
            )

            // Security disclaimer — makes clear that the token alone is not a security boundary.
            Text(
                "Tokens are temporary identifiers used for local peer verification. They do not guarantee cryptographic security on their own.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    navController.navigate(Screen.HandshakeVerification.createRoute(tokenInput))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = tokenInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Link, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Connect", fontWeight = FontWeight.Medium)
            }
        }
    }
}
