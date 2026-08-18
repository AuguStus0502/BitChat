package com.bitchat.diagnostics

import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.bitchat.core.protocol.ProtocolConstants
import com.bitchat.network.ble.BleConnectionManager
import com.bitchat.network.routing.RoutingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Collects and exposes runtime diagnostics for the BitChat application.
 *
 * Provides real-time metrics for the Diagnostics screen and experiment mode,
 * including network statistics, peer connectivity, storage usage, battery
 * consumption, BLE state, and protocol-level counters.
 *
 * The diagnostics engine runs a periodic polling loop that updates all metrics
 * every [POLL_INTERVAL_MS] milliseconds. All values are exposed as immutable
 * [StateFlow] properties for safe observation from Compose UI.
 *
 * Data export generates a structured text report suitable for academic analysis
 * and includes all collected metrics with timestamps.
 */
class DiagnosticsManager(
    private val context: Context,
    private val routingManager: RoutingManager?,
    private val connectionManager: BleConnectionManager?
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _packetsRouted = MutableStateFlow(0L)
    val packetsRouted: StateFlow<Long> = _packetsRouted.asStateFlow()

    private val _packetsDropped = MutableStateFlow(0L)
    val packetsDropped: StateFlow<Long> = _packetsDropped.asStateFlow()

    private val _bytesTransferred = MutableStateFlow(0L)
    val bytesTransferred: StateFlow<Long> = _bytesTransferred.asStateFlow()

    private val _averageLatencyMs = MutableStateFlow(0L)
    val averageLatencyMs: StateFlow<Long> = _averageLatencyMs.asStateFlow()

    private val _connectedPeerCount = MutableStateFlow(0)
    val connectedPeerCount: StateFlow<Int> = _connectedPeerCount.asStateFlow()

    private val _discoveredPeerCount = MutableStateFlow(0)
    val discoveredPeerCount: StateFlow<Int> = _discoveredPeerCount.asStateFlow()

    private val _relayNodeCount = MutableStateFlow(0)
    val relayNodeCount: StateFlow<Int> = _relayNodeCount.asStateFlow()

    private val _batteryPercent = MutableStateFlow(0)
    val batteryPercent: StateFlow<Int> = _batteryPercent.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _storageUsedMB = MutableStateFlow(0L)
    val storageUsedMB: StateFlow<Long> = _storageUsedMB.asStateFlow()

    private val _storageFreeMB = MutableStateFlow(0L)
    val storageFreeMB: StateFlow<Long> = _storageFreeMB.asStateFlow()

    private val _uptimeMs = MutableStateFlow(0L)
    val uptimeMs: StateFlow<Long> = _uptimeMs.asStateFlow()

    private val _bleEnabled = MutableStateFlow(false)
    val bleEnabled: StateFlow<Boolean> = _bleEnabled.asStateFlow()

    private val _scanActive = MutableStateFlow(false)
    val scanActive: StateFlow<Boolean> = _scanActive.asStateFlow()

    private val _advertiseActive = MutableStateFlow(false)
    val advertiseActive: StateFlow<Boolean> = _advertiseActive.asStateFlow()

    private val _experimentMode = MutableStateFlow(false)
    val experimentMode: StateFlow<Boolean> = _experimentMode.asStateFlow()

    private val _experimentLog = MutableStateFlow<List<ExperimentEntry>>(emptyList())
    val experimentLog: StateFlow<List<ExperimentEntry>> = _experimentLog.asStateFlow()

    private var startTimeMs = System.currentTimeMillis()

    init {
        startPolling()
    }

    private fun startPolling() {
        scope.launch {
            while (true) {
                collectMetrics()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun collectMetrics() {
        _uptimeMs.value = System.currentTimeMillis() - startTimeMs
        collectBatteryMetrics()
        collectStorageMetrics()
        collectBleMetrics()
        collectPeerMetrics()
    }

    private fun collectBatteryMetrics() {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        bm?.let {
            _batteryPercent.value = it.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            _isCharging.value = it.isCharging
        }
    }

    private fun collectStorageMetrics() {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalMB = (stat.blockCountLong * blockSize) / (1024 * 1024)
            val freeMB = (stat.availableBlocksLong * blockSize) / (1024 * 1024)
            _storageUsedMB.value = totalMB - freeMB
            _storageFreeMB.value = freeMB
        } catch (_: Exception) { }
    }

    private fun collectBleMetrics() {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        _bleEnabled.value = bm?.adapter?.isEnabled == true
    }

    private fun collectPeerMetrics() {
        _connectedPeerCount.value = connectionManager?.activeConnectionCount ?: 0
    }

    /** Enable or disable experiment mode for detailed metric logging. */
    fun setExperimentMode(enabled: Boolean) {
        _experimentMode.value = enabled
        if (enabled) startTimeMs = System.currentTimeMillis()
    }

    /** Log an experiment event with a timestamp. */
    fun logExperiment(label: String, value: String) {
        if (!_experimentMode.value) return
        val entry = ExperimentEntry(
            timestamp = System.currentTimeMillis(),
            label = label,
            value = value
        )
        _experimentLog.value = _experimentLog.value + entry
    }

    /** Clear the experiment log. */
    fun clearExperimentLog() {
        _experimentLog.value = emptyList()
    }

    /** Increment the routed packet counter. */
    fun incrementPacketsRouted() {
        _packetsRouted.value++
    }

    /** Increment the dropped packet counter. */
    fun incrementPacketsDropped() {
        _packetsDropped.value++
    }

    /** Add to the total bytes transferred. */
    fun addBytesTransferred(bytes: Long) {
        _bytesTransferred.value += bytes
    }

    /** Generate a plain-text diagnostics report for export. */
    fun generateReport(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val now = dateFormat.format(Date())
        val uptimeFormatted = formatDuration(_uptimeMs.value)

        return buildString {
            appendLine("=== BitChat Diagnostics Report ===")
            appendLine("Generated: $now")
            appendLine()
            appendLine("-- Device --")
            appendLine("  Model:     ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("  Android:   ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("  Protocol:  v${ProtocolConstants.PROTOCOL_VERSION}")
            appendLine()
            appendLine("-- Battery --")
            appendLine("  Level:     ${_batteryPercent.value}%")
            appendLine("  Charging:  ${_isCharging.value}")
            appendLine()
            appendLine("-- Storage --")
            appendLine("  Used:      ${_storageUsedMB.value} MB")
            appendLine("  Free:      ${_storageFreeMB.value} MB")
            appendLine()
            appendLine("-- Network --")
            appendLine("  Routed:    ${_packetsRouted.value} packets")
            appendLine("  Dropped:   ${_packetsDropped.value} packets")
            appendLine("  Bytes:     ${_bytesTransferred.value}")
            appendLine("  Latency:   ${_averageLatencyMs.value} ms avg")
            appendLine()
            appendLine("-- BLE --")
            appendLine("  Enabled:   ${_bleEnabled.value}")
            appendLine("  Scanning:  ${_scanActive.value}")
            appendLine("  Advertise: ${_advertiseActive.value}")
            appendLine("  Peers:     ${_connectedPeerCount.value}")
            appendLine()
            appendLine("-- Uptime --")
            appendLine("  Duration:  $uptimeFormatted")
            appendLine()
            if (_experimentLog.value.isNotEmpty()) {
                appendLine("-- Experiment Log --")
                _experimentLog.value.forEach { entry ->
                    val ts = dateFormat.format(Date(entry.timestamp))
                    appendLine("  [$ts] ${entry.label}: ${entry.value}")
                }
            }
            appendLine("=== End Report ===")
        }
    }

    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = (ms / (1000 * 60 * 60)) % 24
        return "${hours}h ${minutes}m ${seconds}s"
    }

    fun destroy() {
        scope.cancel()
    }

    companion object {
        private const val POLL_INTERVAL_MS = 5000L
    }
}

/** A single experiment log entry with timestamp. */
data class ExperimentEntry(
    val timestamp: Long,
    val label: String,
    val value: String
)
