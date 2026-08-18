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
import com.bitchat.core.models.SosPriority
import com.bitchat.ui.components.PriorityBadge
import com.bitchat.ui.components.SignalStrength
import com.bitchat.ui.components.SignalWarmthIndicator
import com.bitchat.ui.theme.*
import com.bitchat.ui.viewmodels.SosViewModel
import com.bitchat.core.utils.TimeUtils

/**
 * Full-detail view of a single SOS beacon, reachable from [NearbySosFeedScreen].
 *
 * Displays comprehensive information about the selected beacon, including
 * its origin, condition, age, hop count, and a **signal warmth** indicator.
 * Signal warmth is a BitChat-specific metric that represents relative mesh
 * signal strength — it does *not* indicate physical distance.
 *
 * Disaster-resilience context:
 * - Users tap a beacon card in [NearbySosFeedScreen] to reach this screen.
 * - The route carries a `beaconId` path argument used to look up the beacon.
 *
 * **Note:** All field values shown here are placeholder data for UI
 * verification. In a later phase, a ViewModel will resolve the beacon ID
 * against the mesh service and populate the screen with live data.
 *
 * @param beaconId      Unique identifier of the SOS beacon, passed as a nav argument.
 * @param navController Used to navigate back to the feed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosDetailScreen(beaconId: String, navController: NavController, viewModel: SosViewModel = viewModel()) {

    val activeBeacons by viewModel.activeBeacons.collectAsStateWithLifecycle()
    val beacon = activeBeacons.find { it.beaconId == beaconId }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SOS Detail") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Header: beacon title + priority badge ────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SOS Beacon", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (beacon != null) {
                    PriorityBadge(priority = beacon.priority)
                }
            }

            // ── Beacon info card ────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow("From", beacon?.senderName ?: "Unknown peer")
                    DetailRow("Condition", beacon?.condition ?: "Unknown")
                    DetailRow("Age", beacon?.let { TimeUtils.formatAge(it.timestamp) } ?: "Unknown")
                    DetailRow("Hop count", beacon?.hopCount?.toString() ?: "0")
                }
            }

            // ── Signal warmth card ──────────────────────────────────────
            // Visual indicator of relative mesh signal strength. "IMPROVING"
            // means the beacon is being heard more strongly over successive
            // hops. This helps responders gauge proximity without exposing
            // exact GPS coordinates.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Signal Warmth", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    SignalWarmthIndicator(strength = SignalStrength.IMPROVING)
                }
            }

            // ── Disclaimer text ─────────────────────────────────────────
            // Prevents users from misinterpreting signal warmth as physical
            // distance, which could lead to incorrect rescue prioritisation.
            Text(
                "Signal warmth indicates relative signal strength. It does not represent physical distance.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Reusable label–value row used in detail cards throughout the SOS screens.
 *
 * @param label The field name displayed on the left (e.g. "From").
 * @param value The field value displayed on the right (e.g. "Unknown peer").
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}
