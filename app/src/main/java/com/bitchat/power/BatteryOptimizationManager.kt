package com.bitchat.power

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages BLE duty-cycle optimization to maximize battery life during mesh operation.
 *
 * BLE scanning and advertising are the most power-intensive operations in BitChat.
 * This manager dynamically adjusts scan/advertising intervals based on:
 * - Current battery level
 * - Whether the device is charging
 * - Number of connected peers
 * - User-configured power profile (Performance / Balanced / Power Saver)
 *
 * ### Power Profiles
 * | Profile      | Scan Interval | Advertising | Max Peers |
 * |-------------|---------------|-------------|-----------|
 * | Performance | 1s            | Continuous  | Unlimited |
 * | Balanced    | 5s            | 30s on/off  | 8         |
 * | Power Saver | 30s           | 60s on/off  | 4         |
 *
 * ### Battery-Aware Adaptation
 * When battery drops below critical thresholds, the manager overrides the
 * configured profile with more aggressive power saving:
 * - Below 20%: Force Power Saver mode
 * - Below 10%: Reduce scan window to 50% duty cycle
 * - Below 5%: Stop all scanning, maintain advertising only
 *
 * ### Security Trade-offs
 * Reduced scanning intervals increase the time to discover new peers, which
 * slightly reduces the mesh's responsiveness. This is an acceptable trade-off
 * for extended battery life in disaster scenarios where power is scarce.
 */
class BatteryOptimizationManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    // ─── Observed State ────────────────────────────────────────────────

    private val _powerProfile = MutableStateFlow(PowerProfile.BALANCED)
    val powerProfile: StateFlow<PowerProfile> = _powerProfile.asStateFlow()

    private val _batteryPercent = MutableStateFlow(100)
    val batteryPercent: StateFlow<Int> = _batteryPercent.asStateFlow()

    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

    private val _effectiveScanIntervalMs = MutableStateFlow(SCAN_INTERVAL_BALANCED_MS)
    val effectiveScanIntervalMs: StateFlow<Long> = _effectiveScanIntervalMs.asStateFlow()

    private val _isBleEnabled = MutableStateFlow(false)
    val isBleEnabled: StateFlow<Boolean> = _isBleEnabled.asStateFlow()

    private val _isPowerSaveActive = MutableStateFlow(false)
    val isPowerSaveActive: StateFlow<Boolean> = _isPowerSaveActive.asStateFlow()

    // ─── Battery Receiver ──────────────────────────────────────────────

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (scale > 0) {
                _batteryPercent.value = (level * 100) / scale
                _isCharging.value = intent.getIntExtra(
                    BatteryManager.EXTRA_STATUS, -1
                ) == BatteryManager.BATTERY_STATUS_CHARGING
            }
        }
    }

    init {
        // Register for battery broadcasts
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)

        // Query initial BLE state
        _isBleEnabled.value = bluetoothManager?.adapter?.isEnabled == true

        // Start battery-aware duty cycle monitoring
        startDutyCycleMonitor()
    }

    /**
     * Set the user-configured power profile.
     *
     * @param profile The desired power profile.
     */
    fun setPowerProfile(profile: PowerProfile) {
        _powerProfile.value = profile
        recalculateScanInterval()
    }

    /**
     * Get the recommended scan interval in milliseconds based on current state.
     *
     * @return Scan interval in ms.
     */
    fun getRecommendedScanInterval(): Long = _effectiveScanIntervalMs.value

    /**
     * Get the recommended advertising duty cycle (on/off ratio).
     *
     * @return Pair of (advertising-on ms, advertising-off ms).
     */
    fun getRecommendedAdvertisingCycle(): Pair<Long, Long> {
        if (_isCharging.value) return ADVERTISING_ON_MS to 0L // Always on when charging

        return when (_powerProfile.value) {
            PowerProfile.PERFORMANCE -> ADVERTISING_ON_MS to 0L
            PowerProfile.BALANCED -> 30_000L to 15_000L
            PowerProfile.POWER_SAVER -> 60_000L to 60_000L
        }
    }

    /**
     * Get the maximum number of concurrent peer connections recommended
     * for the current power profile and battery state.
     */
    fun getMaxRecommendedPeers(): Int {
        if (_batteryPercent.value <= 5) return 1
        if (_batteryPercent.value <= 10) return 2

        return when (_powerProfile.value) {
            PowerProfile.PERFORMANCE -> Int.MAX_VALUE
            PowerProfile.BALANCED -> 8
            PowerProfile.POWER_SAVER -> 4
        }
    }

    /**
     * Check if the device is in a critical battery state.
     *
     * @return True if battery is below 10% and not charging.
     */
    fun isCriticalBattery(): Boolean = _batteryPercent.value <= 10 && !_isCharging.value

    /** Release resources. */
    fun destroy() {
        try { context.unregisterReceiver(batteryReceiver) } catch (_: Exception) { }
        scope.cancel()
    }

    // ─── Internal Logic ────────────────────────────────────────────────

    private fun startDutyCycleMonitor() {
        scope.launch {
            while (true) {
                recalculateScanInterval()
                delay(DUTY_CYCLE_EVALUATION_INTERVAL_MS)
            }
        }
    }

    private fun recalculateScanInterval() {
        // Battery override: force power saver at low battery
        if (_batteryPercent.value <= 20 && !_isCharging.value) {
            _isPowerSaveActive.value = true
        } else {
            _isPowerSaveActive.value = false
        }

        val baseInterval = if (_isPowerSaveActive.value) {
            SCAN_INTERVAL_SAVER_MS
        } else {
            when (_powerProfile.value) {
                PowerProfile.PERFORMANCE -> SCAN_INTERVAL_PERFORMANCE_MS
                PowerProfile.BALANCED -> SCAN_INTERVAL_BALANCED_MS
                PowerProfile.POWER_SAVER -> SCAN_INTERVAL_SAVER_MS
            }
        }

        // Critical battery: reduce scanning to 50% duty cycle
        val adjustedInterval = if (_batteryPercent.value <= 10 && !_isCharging.value) {
            baseInterval * 2
        } else {
            baseInterval
        }

        _effectiveScanIntervalMs.value = adjustedInterval
    }

    companion object {
        /** Scan intervals for each power profile. */
        private const val SCAN_INTERVAL_PERFORMANCE_MS = 1_000L
        private const val SCAN_INTERVAL_BALANCED_MS = 5_000L
        private const val SCAN_INTERVAL_SAVER_MS = 30_000L

        /** Advertising on-time for non-always-on profiles. */
        private const val ADVERTISING_ON_MS = 30_000L

        /** How often to re-evaluate the duty cycle (every 30 seconds). */
        private const val DUTY_CYCLE_EVALUATION_INTERVAL_MS = 30_000L
    }
}

/**
 * User-configurable power profile controlling BLE duty cycle.
 */
enum class PowerProfile {
    /** Maximum scan frequency, always-on advertising. Highest battery usage. */
    PERFORMANCE,

    /** Moderate scan frequency, periodic advertising. Balanced battery usage. */
    BALANCED,

    /** Reduced scan frequency, infrequent advertising. Lowest battery usage. */
    POWER_SAVER
}
