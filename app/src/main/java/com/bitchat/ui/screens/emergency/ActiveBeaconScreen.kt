package com.bitchat.ui.screens.emergency

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
import com.bitchat.ui.theme.*
import com.bitchat.core.utils.TimeUtils
import com.bitchat.ui.viewmodels.SosViewModel

/**
 * Live status screen shown after the user has broadcast an SOS beacon.
 *
 * This is the third and final screen in the emergency SOS flow:
 * 1. [SosComposerScreen] — user composes the distress message.
 * 2. [SosConfirmationScreen] — user reviews and confirms the broadcast.
 * 3. **ActiveBeaconScreen** (this screen) — displays real-time beacon metrics.
 *
 * The screen shows a running elapsed-time counter, the beacon's TTL (time to
 * live), relay statistics, and an acknowledgement count. In a later phase these
 * metrics will be fed by the mesh networking layer; placeholder values are used
 * for UI verification today.
 *
 * The user can stop the beacon at any time via the primary action button, which
 * navigates back to the previous screen.
 *
 * @param beaconId   Unique identifier for the active SOS beacon.
 * @param navController Used to navigate back when the beacon is stopped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveBeaconScreen(@Suppress("UNUSED_PARAMETER") beaconId: String, navController: NavController, viewModel: SosViewModel = viewModel()) {

    val localBeacon by viewModel.localBeacon.collectAsStateWithLifecycle()
    @Suppress("UNUSED_VARIABLE")
    val isSosActive by viewModel.isSosActive.collectAsStateWithLifecycle()
    val beacon = localBeacon

    // ── Elapsed time counter ────────────────────────────────────────────
    // Increments every second while the beacon is active. In later phases
    // this value will be driven by the mesh service rather than a local timer.
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds++
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Emergency Beacon") },
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

            // ── Pulsing beacon indicator ────────────────────────────────
            // Large warning icon that visually communicates the active
            // broadcast state. A pulsing animation will be added in a later
            // phase to further draw attention.
            Icon(
                Icons.Default.Warning,
                contentDescription = "Active Beacon",
                modifier = Modifier.size(80.dp),
                tint = EmergencyCritical
            )

            Text(
                text = "BROADCASTING",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = EmergencyCritical
            )

            // ── Beacon statistics card ──────────────────────────────────
            // Displays real-time metrics about the active beacon. Current
            // values are placeholder data that will be replaced with live
            // mesh-layer data in a later phase.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BeaconDetailRow("Elapsed", TimeUtils.formatDuration(elapsedSeconds * 1000))
                    BeaconDetailRow("Expiry", if (beacon != null) TimeUtils.formatDuration(beacon.expiryTime - System.currentTimeMillis()) else "Unknown")
                    BeaconDetailRow("Relay nodes", if (beacon?.relayPermission == true) "Allowed" else "Disabled")
                    BeaconDetailRow("Hop count", beacon?.hopCount?.toString() ?: "0")
                    BeaconDetailRow("Acknowledgements", "0")
                }
            }

            // ── Info banner ─────────────────────────────────────────────
            // Reassures the user that the SOS is actively being broadcast.
            // Uses a calm colour (EmergencyStable) to avoid compounding panic.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = EmergencyStableBg)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = EmergencyStable)
                    Text(
                        "Your SOS is being broadcast to nearby BitChat devices.",
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Stop beacon button ──────────────────────────────────────
            // Terminates the active SOS broadcast and returns the user to
            // the previous screen. In a later phase this will also signal the
            // mesh service to cease transmitting the beacon.
            Button(
                onClick = {
                    viewModel.cancelBeacon()
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyCritical)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop Beacon", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Reusable label–value row for beacon statistics.
 *
 * @param label The metric name displayed on the left (e.g. "Elapsed").
 * @param value The metric value displayed on the right (e.g. "00:05:00").
 */
@Composable
private fun BeaconDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}
