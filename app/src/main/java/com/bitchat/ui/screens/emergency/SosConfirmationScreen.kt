package com.bitchat.ui.screens.emergency

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bitchat.core.models.SosPriority
import com.bitchat.ui.navigation.Screen
import com.bitchat.ui.theme.*
import com.bitchat.ui.viewmodels.SosViewModel

/**
 * Second step of the emergency SOS flow — a final review before broadcast.
 *
 * This screen reads the form data written by [SosComposerScreen] via the
 * previous back-stack entry's `savedStateHandle`, displays a structured
 * summary, and lets the user either confirm (broadcast) or cancel (go back).
 *
 * Disaster-resilience flow:
 * 1. [SosComposerScreen] — user composes the SOS message.
 * 2. **SosConfirmationScreen** (this screen) — user reviews and confirms.
 * 3. [ActiveBeaconScreen] — live beacon status while the SOS is active.
 *
 * Confirming navigates to the home screen, clearing the back stack so the
 * user cannot accidentally re-submit the same beacon.
 *
 * @param navController Used to pop back on cancel or navigate home on confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosConfirmationScreen(navController: NavController, viewModel: SosViewModel = viewModel()) {

    // ── Read form data from ViewModel ───────────────────────────────────
    val priority by viewModel.priority.collectAsStateWithLifecycle()
    val condition by viewModel.condition.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val includeLocation by viewModel.includeLocation.collectAsStateWithLifecycle()
    val allowRelay by viewModel.allowRelay.collectAsStateWithLifecycle()
    val isBroadcasting by viewModel.isBroadcasting.collectAsStateWithLifecycle()
    val broadcastResult by viewModel.broadcastResult.collectAsStateWithLifecycle()

    // Navigate to ActiveBeacon once broadcast succeeds
    LaunchedEffect(broadcastResult) {
        broadcastResult?.let { beacon ->
            viewModel.clearResult()
            navController.navigate(Screen.ActiveBeacon.createRoute(beacon.beaconId)) {
                popUpTo(Screen.Home.route) { inclusive = false }
            }
        }
    }

    // Map each priority level to its colour and uppercase label for display.
    val (priorityColor, priorityLabel) = when (priority) {
        SosPriority.CRITICAL -> Pair(EmergencyCritical, "CRITICAL")
        SosPriority.HELP_NEEDED -> Pair(EmergencyHelpNeeded, "HELP NEEDED")
        SosPriority.STABLE -> Pair(EmergencyStable, "STABLE")
    }

    Scaffold(
        topBar = {
            // The top bar colour matches the chosen priority so the user
            // gets an immediate visual cue about the urgency level.
            TopAppBar(
                title = { Text("Confirm SOS Broadcast") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = priorityColor,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Summary card ────────────────────────────────────────────
            // Displays every field the user entered on the composer screen
            // so they can verify accuracy before broadcasting.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = priorityColor.copy(alpha = 0.1f)),
                border = CardDefaults.outlinedCardBorder().copy(/* border */)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "This SOS will be broadcast to all nearby BitChat devices.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailRow("Priority", priorityLabel)
                    DetailRow("Condition", condition)
                    if (message.isNotBlank()) DetailRow("Message", message)
                    DetailRow("Location", if (includeLocation) "Included" else "Not included")
                    DetailRow("Relay", if (allowRelay) "Allowed" else "Disabled")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Confirm & Broadcast button ──────────────────────────────
            // Finalises the SOS. In a later phase this will trigger the
            // mesh-layer broadcast and transition to [ActiveBeaconScreen].
            Button(
                onClick = { viewModel.broadcastSos() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = priorityColor),
                enabled = !isBroadcasting
            ) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isBroadcasting) "Broadcasting..." else "Confirm & Broadcast",
                    fontWeight = FontWeight.Bold
                )
            }

            // ── Cancel button ───────────────────────────────────────────
            // Returns the user to the composer so they can edit their SOS
            // without discarding the navigation context.
            OutlinedButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}

/**
 * Reusable label–value row used in summary cards throughout the SOS screens.
 *
 * @param label The field name displayed on the left (e.g. "Priority").
 * @param value The field value displayed on the right (e.g. "CRITICAL").
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}
