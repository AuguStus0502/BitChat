package com.bitchat.ui.screens.emergency

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.bitchat.core.models.SosBeacon
import com.bitchat.core.models.SosPriority
import com.bitchat.ui.components.PriorityBadge
import com.bitchat.ui.navigation.Screen
import com.bitchat.ui.theme.*
import com.bitchat.core.utils.TimeUtils
import com.bitchat.ui.viewmodels.SosViewModel

/**
 * Read-only feed that lists all nearby SOS beacons detected via the mesh network.
 *
 * This screen is the primary way for users to discover ongoing emergencies in
 * their vicinity. Each beacon card is tappable and navigates to [SosDetailScreen]
 * for a full breakdown including signal warmth and hop information.
 *
 * Disaster-resilience context:
 * - Beacons are received from other BitChat nodes over the mesh, so this feed
 *   can populate even when the internet is completely unavailable.
 * - The list is sorted by recency (most recent first) by the data layer;
 *   the UI simply renders what it receives.
 *
 * **Note:** The current data source is placeholder/test data used for UI
 * verification. In a later phase this will be replaced with live beacon data
 * from the mesh networking service.
 *
 * @param navController Used to navigate to [SosDetailScreen] when a beacon is tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbySosFeedScreen(navController: NavController, viewModel: SosViewModel = viewModel()) {

    // ── Live beacon data from ViewModel ──────────────────────────────────
    val activeBeacons by viewModel.activeBeacons.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby SOS Feed") },
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
        if (activeBeacons.isEmpty()) {

            // ── Empty state ─────────────────────────────────────────────
            // Shown when no beacons are currently in range. Provides visual
            // feedback that the mesh scan is active but found nothing.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No active SOS beacons nearby", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {

            // ── Beacon list ─────────────────────────────────────────────
            // Renders each beacon as a tappable card. Tapping navigates to
            // the detail screen using the beacon's unique ID as a route arg.
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(activeBeacons) { beacon ->
                    SosBeaconCard(
                        beacon = beacon,
                        onClick = { navController.navigate(Screen.SosDetail.createRoute(beacon.beaconId)) }
                    )
                }
            }
        }
    }
}

/**
 * Compact card representing a single SOS beacon in the nearby feed.
 *
 * Displays the sender's name, priority badge, condition, optional message,
 * relative age, and hop count. Tapping the card triggers navigation to the
 * full detail screen.
 *
 * @param beacon  The [SosBeacon] data model to render.
 * @param onClick Callback invoked when the user taps this card.
 */
@Composable
private fun SosBeaconCard(beacon: SosBeacon, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header row: sender name + priority badge ────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = beacon.senderName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                PriorityBadge(priority = beacon.priority)
            }

            // ── Condition + optional message ────────────────────────────
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = beacon.condition,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (beacon.message.isNotBlank()) {
                Text(
                    text = beacon.message,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Footer row: age + hop count ─────────────────────────────
            // Provides quick context about beacon freshness and how many
            // mesh hops it traversed before reaching this device.
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = TimeUtils.formatAge(beacon.timestamp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Hop: ${beacon.hopCount}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
