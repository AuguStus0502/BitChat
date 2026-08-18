package com.bitchat.network.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.ParcelUuid
import com.bitchat.core.protocol.ProtocolConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

/**
 * Manages BLE GATT connections to nearby BitChat peers.
 *
 * Handles the full connection lifecycle:
 * 1. Connect to a remote device
 * 2. Discover GATT services
 * 3. Find the BitChat characteristic
 * 4. Enable notifications for incoming data
 * 5. Send data via the characteristic
 * 6. Handle disconnection and reconnection
 *
 * Each connection is represented as a [PeerConnection] that provides
 * a Flow of incoming data and methods for sending data.
 */
@Suppress("DEPRECATION")
class BleConnectionManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    private val connections = mutableMapOf<String, PeerConnection>()
    private val _connectionState = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 64)

    /** Observe connection events from all managed connections. */
    val connectionEvents: SharedFlow<ConnectionEvent> = _connectionState.asSharedFlow()

    /** Get the number of currently active connections. */
    val activeConnectionCount: Int get() = connections.size

    /** Check if already connected to a specific device. */
    fun isConnected(address: String): Boolean = connections.containsKey(address)

    /**
     * Initiate a connection to a nearby BLE device.
     *
     * @param address The BLE MAC address of the target device.
     * @return A PeerConnection if connection is initiated, null if already connected or Bluetooth unavailable.
     */
    @SuppressLint("MissingPermission")
    fun connect(address: String): PeerConnection? {
        if (connections.containsKey(address)) return connections[address]
        val adapter = bluetoothAdapter ?: return null
        val device = adapter.getRemoteDevice(address) ?: return null

        _connectionState.tryEmit(ConnectionEvent.Connecting(address))

        val gattCallback = createGattCallback(address)
        val gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)

        val peerConnection = PeerConnection(address, gatt)
        connections[address] = peerConnection
        return peerConnection
    }

    /**
     * Disconnect from a specific peer.
     * Cleans up GATT resources and removes from the active connections map.
     */
    @SuppressLint("MissingPermission")
    fun disconnect(address: String) {
        val connection = connections.remove(address) ?: return
        connection.gatt?.disconnect()
        connection.gatt?.close()
        _connectionState.tryEmit(ConnectionEvent.Disconnected(address))
    }

    /** Send raw bytes to a connected peer via the BitChat characteristic. */
    fun send(address: String, data: ByteArray): Boolean {
        val connection = connections[address] ?: return false
        val characteristic = connection.writeCharacteristic ?: return false
        characteristic.value = data
        return try {
            connection.gatt?.writeCharacteristic(characteristic) == true
        } catch (e: SecurityException) {
            false
        }
    }

    /** Disconnect all connections and release resources. */
    @SuppressLint("MissingPermission")
    fun disconnectAll() {
        connections.values.forEach { conn ->
            conn.gatt?.disconnect()
            conn.gatt?.close()
        }
        connections.clear()
    }

    /** Create the GATT callback handler for a specific peer connection. */
    private fun createGattCallback(address: String): BluetoothGattCallback {
        return object : BluetoothGattCallback() {

            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        _connectionState.tryEmit(ConnectionEvent.Connected(address))
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        connections.remove(address)
                        gatt.close()
                        _connectionState.tryEmit(ConnectionEvent.Disconnected(address))
                    }
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    disconnect(address)
                    return
                }

                // Find the BitChat service and characteristic
                val service = gatt.getService(UUID.fromString(ProtocolConstants.SERVICE_UUID)) ?: run {
                    disconnect(address)
                    return
                }
                val characteristic = service.getCharacteristic(
                    UUID.fromString(ProtocolConstants.CHARACTERISTIC_UUID)
                ) ?: run {
                    disconnect(address)
                    return
                }

                val connection = connections[address] ?: return
                connection.writeCharacteristic = characteristic

                // Enable notifications for incoming data
                gatt.setCharacteristicNotification(characteristic, true)
                val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)

                _connectionState.tryEmit(ConnectionEvent.Ready(address))
            }

            @Deprecated("Deprecated in Java", level = DeprecationLevel.HIDDEN)
            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                val data = characteristic.value ?: return
                _connectionState.tryEmit(ConnectionEvent.DataReceived(address, data))
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    _connectionState.tryEmit(ConnectionEvent.SendFailed(address))
                }
            }
        }
    }
}

/**
 * Represents an active BLE connection to a remote peer.
 */
data class PeerConnection(
    val address: String,
    val gatt: BluetoothGatt?,
    var writeCharacteristic: BluetoothGattCharacteristic? = null
)

/** Events emitted by the BLE connection manager. */
sealed class ConnectionEvent {
    data class Connecting(val address: String) : ConnectionEvent()
    data class Connected(val address: String) : ConnectionEvent()
    data class Ready(val address: String) : ConnectionEvent()
    data class DataReceived(val address: String, val data: ByteArray) : ConnectionEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DataReceived) return false
            return address == other.address && data.contentEquals(other.data)
        }
        override fun hashCode(): Int = 31 * address.hashCode() + data.contentHashCode()
    }
    data class SendFailed(val address: String) : ConnectionEvent()
    data class Disconnected(val address: String) : ConnectionEvent()
}
