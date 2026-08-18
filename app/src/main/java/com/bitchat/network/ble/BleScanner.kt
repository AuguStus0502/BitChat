package com.bitchat.network.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import com.bitchat.core.protocol.ProtocolConstants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce

/**
 * Manages BLE scanning for nearby BitChat peers.
 *
 * Uses Android BLE APIs to discover devices advertising the BitChat service UUID.
 * Scans are debounced to prevent rapid-fire discovery callbacks from flooding
 * the UI layer. Each discovered device is emitted as a [ScanResult] containing
 * the BLE address, RSSI, and advertised service data.
 *
 * The scanner respects Android power-saving by using low-power scan mode
 * by default, switching to low-latency only when actively user-initiated.
 */
class BleScanner(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var currentScanCallback: ScanCallback? = null

    /** Check if Bluetooth LE is available and enabled on this device. */
    fun isBleAvailable(): Boolean {
        return bluetoothAdapter?.isEnabled == true && bleScanner != null
    }

    /**
     * Start scanning for BitChat peers.
     *
     * Returns a Flow that emits unique scan results. The flow automatically
     * cleans up the scan when the collector is cancelled.
     *
     * @param lowLatency If true, use low-latency scan mode (higher power, faster discovery).
     * @return Flow of scan results debounced to 500ms.
     */
    @OptIn(kotlinx.coroutines.FlowPreview::class)
    @SuppressLint("MissingPermission")
    fun startScan(lowLatency: Boolean = false): Flow<ScanResult> = callbackFlow {
        val settings = ScanSettings.Builder()
            .setScanMode(
                if (lowLatency) ScanSettings.SCAN_MODE_LOW_LATENCY
                else ScanSettings.SCAN_MODE_LOW_POWER
            )
            .setReportDelay(0) // Immediate reporting
            .build()

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString(ProtocolConstants.SERVICE_UUID))
                .build()
        )

        currentScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                trySend(result)
            }

            override fun onScanFailed(errorCode: Int) {
                close(Exception("BLE scan failed with error code: $errorCode"))
            }
        }

        bleScanner?.startScan(filters, settings, currentScanCallback)

        awaitClose {
            currentScanCallback?.let { bleScanner?.stopScan(it) }
            currentScanCallback = null
        }
    }.debounce(500)

    /** Stop any active scan immediately. */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        currentScanCallback?.let { bleScanner?.stopScan(it) }
        currentScanCallback = null
    }
}
