package com.bitchat.ui.screens.diagnostics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bitchat.ui.theme.*
import com.bitchat.ui.viewmodels.DiagnosticsViewModel

/**
 * Research diagnostics screen displaying real-time BLE mesh metrics.
 *
 * Accessible from both [HomeScreen] (top bar) and [SettingsScreen].
 * Designed primarily for the BSc research component of the project,
 * providing transparent visibility into the underlying protocol state.
 *
 * ### Displayed Metrics
 * - **Bluetooth LE** — adapter state, scanning/advertising status, peer counts.
 * - **Connection** — transport type, active sessions, message queue depth, latency.
 * - **Messages** — sent/received/relayed/delivered/failed/expired counters.
 * - **Security** — active keys, session count, authentication & replay stats.
 * - **System** — battery level, uptime, protocol & app version.
 *
 * ### Data Sensitivity
 * All displayed values are anonymised operational metrics. Private keys,
 * credentials, and message content are **never** shown or exported.
 *
 * ### Accessibility Notes
 * - Monospace font ([FontFamily.Monospace]) on metric values aids
 *   alignment and scannability for users with dyslexia.
 * - Section headings use primary-coloured text for clear visual grouping.
 * - Export buttons have 48dp+ touch targets and icon labels for TTS.
 *
 * NOTE: All values are currently placeholder/sample data and will be
 * wired to live BLE and session managers in a later phase.
 *
 * @param navController Standard Jetpack Navigation controller; pops back
 *        on back-navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(navController: NavController) {
    val viewModel: DiagnosticsViewModel = viewModel()

    val bleEnabled by viewModel.bleEnabled.collectAsStateWithLifecycle()
    val scanActive by viewModel.scanActive.collectAsStateWithLifecycle()
    val advertiseActive by viewModel.advertiseActive.collectAsStateWithLifecycle()
    val connectedPeerCount by viewModel.connectedPeerCount.collectAsStateWithLifecycle()
    val discoveredPeerCount by viewModel.discoveredPeerCount.collectAsStateWithLifecycle()
    val packetsRouted by viewModel.packetsRouted.collectAsStateWithLifecycle()
    val packetsDropped by viewModel.packetsDropped.collectAsStateWithLifecycle()
    val bytesTransferred by viewModel.bytesTransferred.collectAsStateWithLifecycle()
    val averageLatencyMs by viewModel.averageLatencyMs.collectAsStateWithLifecycle()
    val batteryPercent by viewModel.batteryPercent.collectAsStateWithLifecycle()
    val uptimeMs by viewModel.uptimeMs.collectAsStateWithLifecycle()
    val relayNodeCount by viewModel.relayNodeCount.collectAsStateWithLifecycle()
    val exportConfirmation by viewModel.exportConfirmation.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(exportConfirmation) {
        exportConfirmation?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExportConfirmation()
        }
    }

    val uptimeText = remember(uptimeMs) {
        val totalSeconds = uptimeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        "${minutes}m ${seconds}s"
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Research Diagnostics") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Bluetooth LE Metrics ───────────────────────────────────
            // Low-level BLE adapter state and peer discovery counters.
            DiagnosticsSection("Bluetooth LE") {
                DiagnosticsRow("State", if (bleEnabled) "Enabled" else "Disabled")
                DiagnosticsRow("Scanning", if (scanActive) "Active" else "Inactive")
                DiagnosticsRow("Advertising", if (advertiseActive) "Active" else "Inactive")
                DiagnosticsRow("Connected Peers", connectedPeerCount.toString())
                DiagnosticsRow("Discovered Peers", discoveredPeerCount.toString())
            }

            // ── Connection Metrics ─────────────────────────────────────
            // Session-level statistics including transport, latency, and
            // message queue depth.
            DiagnosticsSection("Connection") {
                DiagnosticsRow("Transport", "BLE")
                DiagnosticsRow("Active Sessions", connectedPeerCount.toString())
                DiagnosticsRow("Queued Messages", packetsDropped.toString())
                DiagnosticsRow("Avg Latency", "$averageLatencyMs ms")
                DiagnosticsRow("Relay Nodes", relayNodeCount.toString())
            }

            // ── Message Counters ───────────────────────────────────────
            // Per-message-lifecycle counters. "Relayed" tracks messages
            // forwarded on behalf of other peers (mesh forwarding).
            DiagnosticsSection("Messages") {
                DiagnosticsRow("Packets Routed", packetsRouted.toString())
                DiagnosticsRow("Packets Dropped", packetsDropped.toString())
                DiagnosticsRow("Bytes Transferred", bytesTransferred.toString())
            }

            // ── Security Metrics ───────────────────────────────────────
            // Cryptographic health indicators. "Replay Rejected" counts
            // packets that failed the nonce/replay-protection check.
            DiagnosticsSection("Security") {
                DiagnosticsRow("BLE Enabled", if (bleEnabled) "Yes" else "No")
                DiagnosticsRow("Connected", connectedPeerCount.toString())
                DiagnosticsRow("Discovered", discoveredPeerCount.toString())
                DiagnosticsRow("Relays", relayNodeCount.toString())
            }

            // ── System Info ────────────────────────────────────────────
            // Device and application metadata.
            DiagnosticsSection("System") {
                DiagnosticsRow("Battery", "$batteryPercent%")
                DiagnosticsRow("Uptime", uptimeText)
                DiagnosticsRow("Protocol Version", "1")
                DiagnosticsRow("App Version", "0.1.0")
            }

            // ── Export Controls ────────────────────────────────────────
            // CSV and JSON export buttons for research data collection.
            // NOTE: Export functionality is a placeholder; the onClick
            // handlers will be implemented in a later phase.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.exportReport() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export Report")
                }
            }

            // Privacy notice: reinforces that exports are safe to share
            Text(
                "Export contains anonymised metrics only. Private keys, credentials, and message content are never exported.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Sectioned card that groups related diagnostics rows under a heading.
 *
 * Provides consistent visual structure (rounded card, primary-coloured title,
 * 8dp spacing before content) for each metric category on [DiagnosticsScreen].
 *
 * @param title   Section heading displayed in primary colour and semi-bold.
 * @param content Composable lambda containing [DiagnosticsRow] instances.
 */
@Composable
private fun DiagnosticsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

/**
 * A single label-value row within a [DiagnosticsSection].
 *
 * Displays the label on the left in regular weight and the value on the
 * right in monospace font for consistent alignment of numeric data.
 *
 * ### Accessibility Notes
 * - Monospace font ([FontFamily.Monospace]) ensures that digits and
 *   units align vertically, aiding quick scanning.
 * - 13sp font size balances readability with information density.
 *
 * @param label Human-readable metric name (e.g. "Avg Latency").
 * @param value Current metric value in monospace (e.g. "0", "--- ms").
 */
@Composable
private fun DiagnosticsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}
