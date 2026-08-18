package com.bitchat.ui.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.le.ScanResult as BleScanResult
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bitchat.app.BitChatApplication
import com.bitchat.core.models.Peer
import com.bitchat.core.models.PeerState
import com.bitchat.core.utils.IdGenerator
import com.bitchat.network.ble.BleConnectionManager
import com.bitchat.network.ble.BleScanner
import com.bitchat.storage.repositories.PeerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel powering the [PeerDiscoveryScreen][com.bitchat.ui.screens.private.PeerDiscoveryScreen]
 * with real BLE scanning and connection management.
 *
 * ### Responsibilities
 * 1. **Scanning** — starts and stops BLE scanning via [BleScanner], converting raw
 *    [android.bluetooth.le.ScanResult] callbacks into [Peer] domain objects that the
 *    Compose UI can render directly.
 * 2. **Peer list** — maintains an observable list of discovered peers sorted by signal
 *    strength (RSSI), updated in real-time as BLE advertisements arrive.
 * 3. **Connection** — delegates connection initiation to [BleConnectionManager] when the
 *    user taps a peer card.
 *
 * ### BLE Scan Lifecycle
 * - Scanning starts automatically on [init] if BLE is available.
 * - The scan [Job] is tracked so it can be cancelled when the ViewModel is cleared.
 * - [startScan] and [stopScan] allow the UI to toggle scanning from the top-bar button.
 * - Scan results are deduplicated by BLE address and debounced at 500 ms by
 *   [BleScanner.startScan].
 *
 * ### Thread Safety
 * - [MutableStateFlow] is used for all exposed state, ensuring thread-safe reads.
 * - BLE scan callbacks arrive on a binder thread; they are emitted directly into the
 *   [MutableStateFlow] which handles synchronisation internally.
 *
 * @param application The running [Application] instance, used to access the shared
 *                    [BitChatApplication.database] and system Bluetooth services.
 */
class PeerDiscoveryViewModel(application: Application) : AndroidViewModel(application) {

    // ── Dependency references ────────────────────────────────────────────

    /** Shared application instance for database and context access. */
    private val app = application as BitChatApplication

    /** BLE scanner for discovering nearby BitChat peers. */
    private val bleScanner = BleScanner(application)

    /** BLE connection manager for initiating peer connections. */
    private val bleConnectionManager = BleConnectionManager(application)

    /** Repository for persisting discovered peers and observing connection state. */
    private val peerRepository = PeerRepository(app.database.peerDao())

    // ── Mutable backing fields ───────────────────────────────────────────

    /**
     * List of peers discovered during the current BLE scan session.
     * Each entry is a [Peer] converted from a raw [android.bluetooth.le.ScanResult].
     * The list is deduplicated by BLE address and sorted by RSSI (strongest first).
     */
    private val _discoveredPeers = MutableStateFlow<List<Peer>>(emptyList())

    /** Whether a BLE scan is currently active. */
    private val _isScanning = MutableStateFlow(false)

    /** Whether the device's Bluetooth adapter is available and enabled. */
    private val _isBleEnabled = MutableStateFlow(false)

    /**
     * Set of BLE addresses already added to [_discoveredPeers] to prevent duplicates.
     * Cleared when a new scan session starts.
     */
    private val seenAddresses = mutableSetOf<String>()

    /** Handle for the current scan coroutine, so it can be cancelled. */
    private var scanJob: Job? = null

    // ── Public read-only StateFlows ──────────────────────────────────────

    /** Observable list of discovered peers, sorted by signal strength. */
    val discoveredPeers: StateFlow<List<Peer>> = _discoveredPeers.asStateFlow()

    /** Observable flag indicating whether BLE scanning is active. */
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    /** Observable BLE adapter availability. */
    val isBleEnabled: StateFlow<Boolean> = _isBleEnabled.asStateFlow()

    // ── Initialisation ───────────────────────────────────────────────────

    init {
        // Check BLE availability on startup
        _isBleEnabled.value = bleScanner.isBleAvailable()

        // Auto-start scanning if BLE is available
        if (_isBleEnabled.value) {
            startScan()
        }
    }

    // ── Actions ──────────────────────────────────────────────────────────

    /**
     * Start scanning for nearby BitChat peers via BLE.
     *
     * If a scan is already active, this is a no-op. Scan results are appended
     * to [discoveredPeers] as they arrive, deduplicated by BLE address.
     *
     * @param lowLatency If true, use low-latency scan mode for faster discovery
     *                   at the cost of higher power consumption.
     */
    fun startScan(lowLatency: Boolean = false) {
        if (_isScanning.value) return
        if (!bleScanner.isBleAvailable()) {
            _isBleEnabled.value = false
            return
        }

        // Reset the deduplication set for a fresh scan session
        seenAddresses.clear()

        scanJob = viewModelScope.launch {
            _isScanning.value = true
            bleScanner.startScan(lowLatency)
                .catch { /* Scan error — BLE may have been disabled mid-scan */ }
                .collect { scanResult ->
                    addOrUpdatePeer(scanResult)
                }
        }
    }

    /**
     * Stop the current BLE scan session.
     *
     * Safe to call even if no scan is active. Clears the scan job and
     * notifies the BLE scanner to release system resources.
     */
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        bleScanner.stopScan()
        _isScanning.value = false
    }

    /**
     * Initiate a BLE connection to a discovered peer.
     *
     * Delegates to [BleConnectionManager.connect] which handles the full GATT
     * connection lifecycle. The peer's state in the database will be updated
     * by the connection-state callbacks.
     *
     * @param address The BLE MAC address of the target peer.
     * @return The [com.bitchat.network.ble.PeerConnection] if connection was initiated,
     *         or null if the address is invalid or Bluetooth is unavailable.
     */
    @SuppressLint("MissingPermission")
    fun connectToPeer(address: String): com.bitchat.network.ble.PeerConnection? {
        // Update the peer's state in the database to CONNECTING
        viewModelScope.launch {
            val peer = peerRepository.getPeerByAddress(address)
            if (peer != null) {
                peerRepository.updateState(peer.peerId, PeerState.CONNECTING)
            }
        }
        return bleConnectionManager.connect(address)
    }

    // ── Private helpers ──────────────────────────────────────────────────

    /**
     * Convert a raw BLE [android.bluetooth.le.ScanResult] into a [Peer] domain object
     * and add it to [_discoveredPeers], or update the existing entry's RSSI if the
     * peer was already seen.
     *
     * This method handles:
     * - Extraction of the device name and BLE address from the scan result.
     * - Deduplication: if the address has been seen before, only the RSSI is updated.
     * - Sorting: the list is re-sorted by RSSI (strongest signal first) after each update.
     */
    private fun addOrUpdatePeer(scanResult: BleScanResult) {
        val address = scanResult.device.address
        val rssi = scanResult.rssi
        val deviceName = scanResult.device.name ?: "Unknown Device"

        val currentList = _discoveredPeers.value.toMutableList()

        // Check if we already have this peer in the list
        val existingIndex = currentList.indexOfFirst { it.bleAddress == address }
        if (existingIndex >= 0) {
            // Update RSSI for existing peer
            val existing = currentList[existingIndex]
            currentList[existingIndex] = existing.copy(
                rssi = rssi,
                lastSeenAt = System.currentTimeMillis()
            )
        } else {
            // Create a new Peer from the scan result
            val newPeer = Peer(
                peerId = IdGenerator.generateId(),
                displayName = deviceName,
                bleAddress = address,
                publicKeyBase64 = "", // Populated during handshake
                discoveredAt = System.currentTimeMillis(),
                lastSeenAt = System.currentTimeMillis(),
                rssi = rssi,
                state = PeerState.DISCOVERED,
                isRelay = true,
                hopCount = 0
            )
            currentList.add(newPeer)
            seenAddresses.add(address)
        }

        // Sort by signal strength (strongest first) and update the state flow
        _discoveredPeers.value = currentList.sortedByDescending { it.rssi ?: Int.MIN_VALUE }
    }

    // ── Cleanup ──────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        // Stop any active scan to release BLE system resources
        stopScan()
    }
}
