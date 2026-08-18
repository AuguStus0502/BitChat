package com.bitchat.ui.screens.panic

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bitchat.security.panic.PanicManager
import com.bitchat.ui.navigation.Screen
import com.bitchat.ui.theme.*
import com.bitchat.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.delay

/**
 * Confirmation screen for the emergency (panic) wipe — a destructive
 * operation that permanently erases all cryptographic keys, session state,
 * private messages, contact tokens, and local identity information.
 *
 * ### Panic Wipe Safety Mechanism
 * This screen implements a **hold-to-confirm** pattern to prevent accidental
 * activation:
 * - The user must **press and hold** the progress indicator for the wipe to
 *   execute. A simple tap does nothing.
 * - A [LinearProgressIndicator] fills as the user holds, providing clear
 *   visual feedback of progress toward confirmation.
 * - Releasing early resets the progress to zero, acting as a safety net.
 * - A prominent "Cancel" button is always available for the user to back out.
 *
 * ### User Journey Context
 * Accessible from [HomeScreen] (Security section) and [SettingsScreen]
 * (Security section). On successful wipe, navigates to
 * [PanicWipeCompleteScreen].
 *
 * ### Accessibility Notes
 * - The top bar uses [EmergencyCritical] (red) background to immediately
 *   convey danger/severity.
 * - The warning icon is 80dp, ensuring it is highly visible.
 * - The "Press and hold" instruction text guides users who may not be
 *   familiar with the hold-to-confirm pattern.
 * - The Cancel button provides an always-available escape route, supporting
 *   users who may have accidentally entered this screen.
 *
 * @param navController Standard Jetpack Navigation controller; pops back on
 *        cancel, or navigates to [PanicWipeCompleteScreen] on confirmed wipe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanicWipeConfirmScreen(navController: NavController) {
    val viewModel: SettingsViewModel = viewModel()
    val wipeState by viewModel.wipeState.collectAsStateWithLifecycle()

    // ── Hold State ────────────────────────────────────────────────────
    // holdProgress: 0.0f → 1.0f as the user presses and holds.
    // isHolding: true while the user's finger is down.
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }

    // Animate the progress bar while the user holds, then trigger wipe.
    LaunchedEffect(isHolding) {
        if (isHolding) {
            val steps = 40
            repeat(steps) { i ->
                holdProgress = (i + 1).toFloat() / steps
                delay(50)
            }
            viewModel.executePanicWipe()
        } else {
            holdProgress = 0f
        }
    }

    // Navigate to PanicWipeComplete when wipe finishes successfully.
    LaunchedEffect(wipeState) {
        if (wipeState is PanicManager.WipeState.Complete) {
            navController.navigate(Screen.PanicWipeComplete.route) {
                popUpTo(Screen.PanicWipeConfirm.route) { inclusive = true }
            }
            viewModel.resetWipeState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Wipe") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmergencyCritical,
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

            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = EmergencyCritical
            )

            Text(
                text = "Emergency Wipe",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = EmergencyCritical
            )

            // ── Destructive Action Manifest ────────────────────────────
            // Explicitly lists every category of data that will be erased
            // so the user can make an informed decision.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = EmergencyCritical.copy(alpha = 0.08f))
            ) {
                Text(
                    text = "This will permanently delete:\n\n" +
                            "  Cryptographic keys\n" +
                            "  Session data\n" +
                            "  Private messages\n" +
                            "  Contact tokens\n" +
                            "  Identity information\n\n" +
                            "This action cannot be undone.",
                    modifier = Modifier.padding(20.dp),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Hold-to-Confirm Button ─────────────────────────────────
            // A LinearProgressIndicator that doubles as a touch target.
            // Uses detectTapGestures with onPress to detect press-and-hold.
            // The progress fills from 0 → 1 while held; releasing early
            // resets it. When holdProgress reaches 1.0, the wipe executes.
            // TODO: Wire holdProgress to an animation that fills over time
            //       and triggers navigation when complete (later phase).
            LinearProgressIndicator(
                progress = { holdProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isHolding = true
                                tryAwaitRelease()
                                isHolding = false
                            }
                        )
                    },
                color = EmergencyCritical,
                trackColor = EmergencyCritical.copy(alpha = 0.2f)
            )

            Text(
                "Press and hold to confirm wipe",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Cancel Button ──────────────────────────────────────────
            // Always visible escape route — full-width for easy tapping.
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}
