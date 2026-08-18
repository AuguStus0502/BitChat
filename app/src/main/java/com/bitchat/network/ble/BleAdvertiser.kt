package com.bitchat.network.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import com.bitchat.core.protocol.ProtocolConstants

/**
 * Manages BLE advertising to make this device discoverable to nearby BitChat peers.
 *
 * Broadcasts a minimal advertisement containing only the BitChat service UUID.
 * This allows other BitChat devices to discover this device during their scans
 * without revealing any identity information in the advertisement itself.
 *
 * Identity exchange occurs only after a connection is established and the
 * handshake protocol completes.
 */
class BleAdvertiser(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleAdvertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser

    private var advertiseCallback: AdvertiseCallback? = null

    /** Check if BLE advertising is supported and enabled. */
    fun isAdvertisingSupported(): Boolean {
        return bluetoothAdapter?.isEnabled == true && bleAdvertiser != null
    }

    /**
     * Start advertising the BitChat service UUID.
     *
     * Uses low-power advertising to minimize battery impact.
     * The advertisement contains only the service UUID - no identity data.
     *
     * @param onError Callback if advertising fails to start.
     */
    @SuppressLint("MissingPermission")
    fun startAdvertising(onError: (String) -> Unit = {}) {
        if (advertiseCallback != null) return // Already advertising

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setConnectable(true) // Allow incoming connections for handshake
            .setTimeout(0) // Advertise indefinitely
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid.fromString(ProtocolConstants.SERVICE_UUID))
            .setIncludeDeviceName(false) // Do not expose device name in advertisement
            .setIncludeTxPowerLevel(false)
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                // Advertising started successfully
            }

            override fun onStartFailure(errorCode: Int) {
                val message = when (errorCode) {
                    ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertisement data too large"
                    ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                    ADVERTISE_FAILED_ALREADY_STARTED -> "Already advertising"
                    ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal advertising error"
                    ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Advertising not supported"
                    else -> "Unknown advertising error: $errorCode"
                }
                advertiseCallback = null
                onError(message)
            }
        }

        bleAdvertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    /** Stop advertising immediately. */
    @SuppressLint("MissingPermission")
    fun stopAdvertising() {
        advertiseCallback?.let { bleAdvertiser?.stopAdvertising(it) }
        advertiseCallback = null
    }
}
