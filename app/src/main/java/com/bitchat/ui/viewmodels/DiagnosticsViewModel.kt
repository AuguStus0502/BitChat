package com.bitchat.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bitchat.app.BitChatApplication
import com.bitchat.diagnostics.DiagnosticsManager
import com.bitchat.diagnostics.ExperimentEntry
import com.bitchat.network.ble.BleConnectionManager
import com.bitchat.network.routing.RoutingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel powering the [DiagnosticsScreen][com.bitchat.ui.screens.diagnostics.DiagnosticsScreen]
 * with real-time BLE mesh metrics and experiment logging.
 *
 * ### Responsibilities
 * 1. **Metrics pass-through** — exposes all [DiagnosticsManager] metric [StateFlow]s as
 *    read-only properties so the Compose UI can collect them directly.
 * 2. **Experiment mode** — provides toggle, log, and clear operations for the research
 *    experiment mode that enables detailed metric tracking.
 * 3. **Report export** — generates a structured plain-text diagnostics report and copies
 *    it to the system clipboard for easy sharing.
 *
 * ### Architecture
 * This ViewModel acts as a thin adapter between [DiagnosticsManager] (which polls system
 * metrics every 5 seconds) and the Compose UI. It does not duplicate any metric logic;
 * all values are read directly from the manager's [StateFlow] properties.
 *
 * ### Thread Safety
 * - [DiagnosticsManager] metrics are updated on [kotlinx.coroutines.Dispatchers.IO] and
 *   exposed as [MutableStateFlow] which is thread-safe.
 * - Clipboard operations are performed on the main thread via [android.os.Handler].
 *
 * @param application The running [Application] instance, used to access the shared
 *                    [BitChatApplication.database] and system services.
 */
class DiagnosticsViewModel(application: Application) : AndroidViewModel(application) {

    // ── Dependency references ────────────────────────────────────────────

    /** Shared application instance for database and context access. */
    private val app = application as BitChatApplication

    /**
     * BLE connection manager, used by [DiagnosticsManager] for the connected-peer metric.
     * Constructed directly as it has no external dependencies beyond the context.
     */
    private val bleConnectionManager = BleConnectionManager(application)

    /**
     * Routing manager, used by [DiagnosticsManager] for packet-routing metrics.
     * Nullable because it may not be initialised in all app states.
     */
    private val routingManager: RoutingManager? = null

    /**
     * DiagnosticsManager — the core metrics collection engine.
     * Polls battery, storage, BLE state, and peer connectivity every 5 seconds.
     */
    val diagnosticsManager = DiagnosticsManager(
        context = application,
        routingManager = routingManager,
        connectionManager = bleConnectionManager
    )

    // ── Additional UI-specific state ─────────────────────────────────────

    /**
     * True when the report generation and clipboard copy operation is in progress.
     * Briefly set to true to drive a loading indicator, then reset.
     */
    private val _isExporting = MutableStateFlow(false)

    /**
     * Confirmation message displayed after a successful export.
     * Reset to null after the UI has displayed the message.
     */
    private val _exportConfirmation = MutableStateFlow<String?>(null)

    // ── Public read-only StateFlows (pass-through from DiagnosticsManager) ──

    /** Total number of packets successfully routed through this node. */
    val packetsRouted: StateFlow<Long> = diagnosticsManager.packetsRouted

    /** Total number of packets dropped (TTL exhausted, duplicate, etc.). */
    val packetsDropped: StateFlow<Long> = diagnosticsManager.packetsDropped

    /** Total bytes transferred over BLE. */
    val bytesTransferred: StateFlow<Long> = diagnosticsManager.bytesTransferred

    /** Average round-trip latency in milliseconds. */
    val averageLatencyMs: StateFlow<Long> = diagnosticsManager.averageLatencyMs

    /** Number of peers currently in a connected state. */
    val connectedPeerCount: StateFlow<Int> = diagnosticsManager.connectedPeerCount

    /** Total number of unique peers discovered (including disconnected). */
    val discoveredPeerCount: StateFlow<Int> = diagnosticsManager.discoveredPeerCount

    /** Number of active relay nodes in the mesh. */
    val relayNodeCount: StateFlow<Int> = diagnosticsManager.relayNodeCount

    /** Current battery level as a percentage (0–100). */
    val batteryPercent: StateFlow<Int> = diagnosticsManager.batteryPercent

    /** Whether the device is currently charging. */
    val isCharging: StateFlow<Boolean> = diagnosticsManager.isCharging

    /** Storage used in megabytes. */
    val storageUsedMB: StateFlow<Long> = diagnosticsManager.storageUsedMB

    /** Storage free in megabytes. */
    val storageFreeMB: StateFlow<Long> = diagnosticsManager.storageFreeMB

    /** Application uptime in milliseconds since the diagnostics engine started. */
    val uptimeMs: StateFlow<Long> = diagnosticsManager.uptimeMs

    /** Whether the Bluetooth adapter is currently enabled. */
    val bleEnabled: StateFlow<Boolean> = diagnosticsManager.bleEnabled

    /** Whether a BLE scan is currently active. */
    val scanActive: StateFlow<Boolean> = diagnosticsManager.scanActive

    /** Whether BLE advertising is currently active. */
    val advertiseActive: StateFlow<Boolean> = diagnosticsManager.advertiseActive

    /** Whether experiment mode is enabled for detailed metric logging. */
    val experimentMode: StateFlow<Boolean> = diagnosticsManager.experimentMode

    /** Log of experiment entries recorded while experiment mode is active. */
    val experimentLog: StateFlow<List<ExperimentEntry>> = diagnosticsManager.experimentLog

    /** Observable exporting-in-progress flag. */
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    /** Observable confirmation message after a successful export. */
    val exportConfirmation: StateFlow<String?> = _exportConfirmation.asStateFlow()

    // ── Actions ──────────────────────────────────────────────────────────

    /**
     * Enable or disable experiment mode.
     *
     * When enabled, [DiagnosticsManager] starts recording experiment entries
     * via [logExperiment] and resets the uptime counter to mark the experiment
     * start time.
     *
     * @param enabled True to enable experiment mode, false to disable.
     */
    fun toggleExperimentMode(enabled: Boolean) {
        diagnosticsManager.setExperimentMode(enabled)
    }

    /**
     * Log an experiment event with a label and value.
     *
     * Events are only recorded when experiment mode is enabled; calls while
     * disabled are silently ignored by [DiagnosticsManager].
     *
     * @param label Short descriptor for the metric (e.g. "latency_p2p").
     * @param value The measured value as a string (e.g. "42ms").
     */
    fun logExperiment(label: String, value: String) {
        diagnosticsManager.logExperiment(label, value)
    }

    /**
     * Generate the full diagnostics report and copy it to the system clipboard.
     *
     * The report is a structured plain-text document generated by
     * [DiagnosticsManager.generateReport] containing all collected metrics with
     * timestamps. After copying, [exportConfirmation] is set with a success message
     * and [isExporting] is reset to false.
     */
    fun exportReport() {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val report = diagnosticsManager.generateReport()

                // Copy the report to the system clipboard
                val clipboard = getApplication<BitChatApplication>()
                    .getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("BitChat Diagnostics Report", report)
                clipboard?.setPrimaryClip(clip)

                _exportConfirmation.value = "Report copied to clipboard"
            } catch (e: Exception) {
                _exportConfirmation.value = "Export failed: ${e.message}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    /**
     * Clear all experiment log entries.
     *
     * Delegates to [DiagnosticsManager.clearExperimentLog] which resets the
     * experiment log [StateFlow] to an empty list.
     */
    fun clearLog() {
        diagnosticsManager.clearExperimentLog()
    }

    /**
     * Clear the export confirmation message.
     *
     * Should be called by the UI after displaying the confirmation
     * toast or snackbar.
     */
    fun clearExportConfirmation() {
        _exportConfirmation.value = null
    }

    // ── Cleanup ──────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        diagnosticsManager.destroy()
    }
}
