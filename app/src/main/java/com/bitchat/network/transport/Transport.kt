package com.bitchat.network.transport

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction for network transports (BLE, Nostr, etc.).
 *
 * Each transport implements send/receive through its specific protocol.
 * The messaging layer uses this interface to remain transport-agnostic.
 */
interface Transport {
    /** Unique identifier for this transport type. */
    val transportId: String

    /** Whether this transport is currently available and functional. */
    fun isAvailable(): Boolean

    /** Start the transport (e.g., begin BLE scanning/advertising). */
    suspend fun start()

    /** Stop the transport gracefully. */
    suspend fun stop()

    /** Send raw bytes through this transport. */
    suspend fun send(data: ByteArray, destinationId: String): Boolean

    /** Observe incoming raw data from this transport. */
    fun observeIncoming(): Flow<TransportData>
}

/**
 * A received data payload from a transport with sender information.
 */
data class TransportData(
    val senderAddress: String,
    val data: ByteArray,
    val rssi: Int? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransportData) return false
        return senderAddress == other.senderAddress && data.contentEquals(other.data)
    }
    override fun hashCode(): Int = 31 * senderAddress.hashCode() + data.contentHashCode()
}
