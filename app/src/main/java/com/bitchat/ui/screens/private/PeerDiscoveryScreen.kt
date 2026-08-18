package com.bitchat.ui.screens.private

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
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
import com.bitchat.core.models.Peer
import com.bitchat.core.models.PeerState
import com.bitchat.ui.navigation.Screen
import com.bitchat.ui.theme.*
import com.bitchat.ui.viewmodels.PeerDiscoveryViewModel

/**
 * Peer Discovery screen for the private-mode communication flow.
 *
 * This is the first step in BitChat's offline peer-to-peer connection sequence:
 * 1. **Peer Discovery** — scan BLE for nearby devices (this screen)
 * 2. **Handshake Verification** — visual fingerprint comparison with the peer
 * 3. **Ephemeral Chat** — end-to-end encrypted, session-scoped messaging
 *
 * The screen initiates a BLE scan and presents discovered peers in a list sorted
 * by signal strength. Each peer card shows the device name, connection state, and
 * an RSSI-derived signal quality indicator.
 *
 * **Phase note:** The peer list currently uses placeholder data. In a later phase,
 * this will be wired to the real BLE scanning service (e.g. via Android BLE APIs
 * or a BLE library) and the list will update reactively as devices appear and
 * disappear from range.
 *
 * Selecting a peer navigates to [HandshakeVerificationScreen] with the peer's ID,
 * beginning the mutual verification handshake.
 *
 * @param navController Navigation controller for transitioning between private-mode screens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerDiscoveryScreen(navController: NavController) {
    val viewModel: PeerDiscoveryViewModel = viewModel()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val discoveredPeers by viewModel.discoveredPeers.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby Peers") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                // Toggle scanning on/off — icon swaps between Stop and Refresh
                // to reflect the current scan state.
                actions = {
                    IconButton(onClick = {
                        if (isScanning) viewModel.stopScan() else viewModel.startScan()
                    }) {
                        Icon(
                            if (isScanning) Icons.Default.Stop else Icons.Default.Refresh,
                            contentDescription = if (isScanning) "Stop scanning" else "Start scanning"
                        )
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
            // Animated scanning indicator card — only visible while the scan is active.
            if (isScanning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlueLight.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Spinning indeterminate progress indicator signals an ongoing BLE scan.
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Scanning for nearby devices...", fontSize = 14.sp)
                    }
                }
            }

            // Empty-state: shown when no peers have been discovered yet.
            if (discoveredPeers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.BluetoothSearching,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No peers found nearby", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "Ensure Bluetooth is enabled on other devices",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Peer list — each item is a tappable [PeerCard] that initiates the
                // handshake verification flow with the selected peer.
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(discoveredPeers) { peer ->
                        PeerCard(
                            peer = peer,
                            onClick = {
                                peer.bleAddress?.let { viewModel.connectToPeer(it) }
                                navController.navigate(Screen.HandshakeVerification.createRoute(peer.peerId))
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays a single discovered peer as a tappable card.
 *
 * The card shows:
 * - A person icon with the device's display name.
 * - The current [PeerState] (e.g. DISCOVERED, CONNECTED).
 * - An RSSI-based signal strength label ("Strong", "Improving", "Weak")
 *   using the [SignalStrong], [SignalImproving], and [SignalWeak] theme colors.
 *
 * RSSI thresholds:
 * - **Strong**  : > -50 dBm
 * - **Improving**: > -70 dBm
 * - **Weak**    : ≤ -70 dBm
 *
 * Tapping the card triggers the [onClick] callback, which typically navigates
 * to [HandshakeVerificationScreen].
 *
 * @param peer The [Peer] data model to display.
 * @param onClick Callback invoked when the user taps this peer card.
 */
@Composable
private fun PeerCard(peer: Peer, onClick: () -> Unit) {
    // Map raw RSSI (dBm) to a human-readable signal label.
    val signalLabel = when {
        (peer.rssi ?: -100) > -50 -> "Strong"
        (peer.rssi ?: -100) > -70 -> "Improving"
        else -> "Weak"
    }
    // Map RSSI to a corresponding theme color for visual clarity.
    val signalColor = when {
        (peer.rssi ?: -100) > -50 -> SignalStrong
        (peer.rssi ?: -100) > -70 -> SignalImproving
        else -> SignalWeak
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            // Peer identity column — name and current connection state.
            Column(modifier = Modifier.weight(1f)) {
                Text(peer.displayName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text(
                    text = peer.state.name,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Signal quality column — label and raw dBm reading.
            Column(horizontalAlignment = Alignment.End) {
                Text(signalLabel, fontSize = 12.sp, color = signalColor, fontWeight = FontWeight.Medium)
                Text("${peer.rssi ?: 0} dBm", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
