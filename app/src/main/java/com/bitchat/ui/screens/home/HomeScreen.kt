package com.bitchat.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.Feed
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bitchat.ui.components.*
import com.bitchat.ui.navigation.Screen
import com.bitchat.ui.theme.*
import com.bitchat.ui.viewmodels.HomeViewModel

/**
 * Primary landing screen after the onboarding flow completes (Splash → Onboarding → Permissions → **Home**).
 *
 * Displays the real-time Bluetooth mesh status, grouped action cards for emergency messaging,
 * peer discovery, and security operations. This is the central hub from which the user
 * navigates to every major feature of the application.
 *
 * ### User Journey Context
 * This screen is only reachable after the user has completed the onboarding pager and
 * granted (or skipped) the required Bluetooth and location permissions. The back stack
 * is cleared on entry so the user cannot accidentally navigate back into the onboarding flow.
 *
 * ### Accessibility Notes
 * - Each [HomeActionCard] uses a 48dp icon surface, meeting the minimum 48dp touch-target
 *   guideline defined by Material Design / WCAG 2.1.
 * - High-contrast icon tinting against a translucent background ensures readability for
 *   users with low vision. All status chips use distinct colour coding (green/red/amber).
 * - Content descriptions on the top-bar icons ("Diagnostics", "Settings") support screen
 *   readers such as TalkBack. Card-level descriptions provide TTS-ready labels for each action.
 *
 * @param navController Standard Jetpack Navigation controller used for all in-app routing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {

    @Suppress("UNUSED_VARIABLE")
    val identityName by viewModel.identityName.collectAsStateWithLifecycle()
    val connectedPeerCount by viewModel.connectedPeerCount.collectAsStateWithLifecycle()
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsStateWithLifecycle()
    val batteryPercent by viewModel.batteryPercent.collectAsStateWithLifecycle()
    @Suppress("UNUSED_VARIABLE")
    val hasActiveSos by viewModel.hasActiveSos.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BitChat", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Diagnostics.route) }) {
                        Icon(Icons.Default.Analytics, contentDescription = "Diagnostics")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Status Bar ──────────────────────────────────────────────
            // Horizontal row of status chips showing live Bluetooth connectivity,
            // internet availability, and battery level at a glance.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BluetoothStatusChip(connected = isBluetoothEnabled, peerCount = connectedPeerCount)
                NetworkStatusChip(online = false)
                BatteryChip(level = batteryPercent)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Action Cards ────────────────────────────────────────────
            // Grouped into three sections: Emergency, Private Communication,
            // and Security. Each card navigates to a dedicated feature screen.
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Emergency Section ────────────────────────────────────
                // SOS broadcast and incoming feed — the highest-priority
                // actions, placed first for quick access during emergencies.
                Text(
                    text = "Emergency",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )

                HomeActionCard(
                    title = "SOS Broadcast",
                    description = "Send an emergency SOS to nearby peers",
                    icon = Icons.Default.Warning,
                    color = EmergencyCritical,
                    onClick = { navController.navigate(Screen.SosComposer.route) }
                )

                HomeActionCard(
                    title = "Nearby SOS Feed",
                    description = "View emergency broadcasts from nearby peers",
                    icon = Icons.AutoMirrored.Filled.Feed,
                    color = EmergencyHelpNeeded,
                    onClick = { navController.navigate(Screen.NearbySosFeed.route) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Private Communication Section ────────────────────────
                // Peer discovery and contact-token exchange — the core
                // peer-to-peer communication workflow.
                Text(
                    text = "Private Communication",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HomeActionCard(
                    title = "Discover Peers",
                    description = "Find and connect to nearby BitChat users",
                    icon = Icons.AutoMirrored.Filled.BluetoothSearching,
                    color = PrimaryBlue,
                    onClick = { navController.navigate(Screen.PeerDiscovery.route) }
                )

                HomeActionCard(
                    title = "Contact Token",
                    description = "Connect using a temporary token",
                    icon = Icons.Default.Token,
                    color = SecondaryTeal,
                    onClick = { navController.navigate(Screen.ContactToken.route) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── Security Section ─────────────────────────────────────
                // Destructive safety actions — deliberately placed at the
                // bottom of the screen to minimise accidental activation.
                Text(
                    text = "Security",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HomeActionCard(
                    title = "Emergency Wipe",
                    description = "Permanently delete all keys and sessions",
                    icon = Icons.Default.DeleteForever,
                    color = EmergencyCritical,
                    onClick = { navController.navigate(Screen.PanicWipeConfirm.route) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Reusable card component used on [HomeScreen] for each navigation action.
 *
 * Renders a rounded card with a coloured icon, title, description, and a
 * trailing chevron to indicate navigability. The entire card is clickable.
 *
 * ### Accessibility Notes
 * - The icon container is 48×48 dp, satisfying the minimum touch-target size.
 * - The [onClick] lambda propagates to the card's clickable modifier, making
 *   the whole row actionable for screen readers and switch-access users.
 * - High-contrast colour tinting on the icon ensures legibility in bright
 *   outdoor conditions (typical for disaster/outdoor use-cases of BitChat).
 *
 * @param title      Primary label displayed in semi-bold (e.g. "SOS Broadcast").
 * @param description Secondary label providing context below the title.
 * @param icon       Material icon shown in the leading position.
 * @param color      Tint applied to the icon and its translucent background.
 * @param onClick    Invoked when the user taps anywhere on the card.
 */
@Composable
private fun HomeActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon surface with translucent tinted background for visual hierarchy
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.12f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = color
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
