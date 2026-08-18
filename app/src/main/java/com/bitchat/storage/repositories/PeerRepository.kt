package com.bitchat.storage.repositories

import com.bitchat.core.models.Peer
import com.bitchat.core.models.PeerState
import com.bitchat.storage.database.PeerDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository for managing discovered Bluetooth peers.
 * Provides a clean API over the PeerDao, filtering stale peers
 * and maintaining a maximum peer list to prevent memory exhaustion.
 */
class PeerRepository(private val peerDao: PeerDao) {

    /** Observe all known peers sorted by most recently seen. */
    fun observeAllPeers(): Flow<List<Peer>> = peerDao.getAllPeers()

    /** Observe only peers currently in a connected state. */
    fun observeConnectedPeers(): Flow<List<Peer>> =
        peerDao.getPeersByStates(listOf(PeerState.CONNECTED, PeerState.AUTHENTICATED))

    /** Observe the count of currently connected peers. */
    fun observeConnectedCount(): Flow<Int> = peerDao.getConnectedPeerCount()

    /** Find a specific peer by their unique identifier. */
    suspend fun getPeerById(peerId: String): Peer? = peerDao.getPeerById(peerId)

    /** Find a peer by their BLE MAC address. */
    suspend fun getPeerByAddress(address: String): Peer? = peerDao.getPeerByAddress(address)

    /** Upsert a peer - insert if new, update if exists. */
    suspend fun upsertPeer(peer: Peer) = peerDao.insertPeer(peer)

    /** Update only the RSSI signal strength and last-seen timestamp. */
    suspend fun updateRssi(peerId: String, rssi: Int) {
        peerDao.updatePeerRssi(peerId, rssi, System.currentTimeMillis())
    }

    /** Update a peer's connection state (e.g., CONNECTING → CONNECTED). */
    suspend fun updateState(peerId: String, state: PeerState) {
        val peer = peerDao.getPeerById(peerId) ?: return
        peerDao.updatePeer(peer.copy(state = state, lastSeenAt = System.currentTimeMillis()))
    }

    /** Remove peers not seen within the given threshold. Default: 5 minutes. */
    suspend fun pruneStalePeers(maxAgeMs: Long = 5 * 60 * 1000L) {
        peerDao.deleteStalePeers(System.currentTimeMillis() - maxAgeMs)
    }

    /** Remove a specific peer (e.g., after explicit disconnect). */
    suspend fun removePeer(peer: Peer) = peerDao.deletePeer(peer)

    /** Remove all peers (used during panic wipe). */
    suspend fun clearAll() = peerDao.deleteAll()
}
