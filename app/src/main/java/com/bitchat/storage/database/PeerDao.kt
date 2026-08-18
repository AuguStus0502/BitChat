package com.bitchat.storage.database

import androidx.room.*
import com.bitchat.core.models.Peer
import com.bitchat.core.models.PeerState
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing [Peer] entities in the local Room database.
 *
 * Peers represent other BitChat users discovered over BLE mesh networking.
 * Each peer is identified by a unique [Peer.peerId] and tracked by its BLE
 * MAC address, connection state, signal strength (RSSI), and recency.
 *
 * ## Threading model
 * - **Reactive queries** (returning [Flow]) are long-lived and executed on a
 *   background thread by Room's coroutine infrastructure. Collectors must
 *   switch to an appropriate dispatcher if they touch the UI.
 * - **Suspend functions** execute a single write or read on Room's write
 *   dispatcher and can be called from any coroutine scope.
 *
 * ## Data lifecycle
 * Stale peers that have not been seen within a configurable threshold are
 * periodically purged via [deleteStalePeers] to prevent unbounded growth of
 * the peer table. The connected-peer count exposed by [getConnectedPeerCount]
 * drives UI indicators such as the mesh status badge.
 */
@Dao
interface PeerDao {

    /**
     * Returns a reactive stream of every known peer, ordered by most recently
     * seen first.
     *
     * Use this to populate the main peer-list UI. The [Flow] will re-emit
     * whenever the underlying table changes (inserts, updates, or deletes).
     *
     * @return [Flow] emitting the full, ordered peer list on every table change.
     */
    @Query("SELECT * FROM peers ORDER BY lastSeenAt DESC")
    fun getAllPeers(): Flow<List<Peer>>

    /**
     * Fetches a single peer by its unique identifier.
     *
     * Returns `null` when no matching peer exists — callers should handle the
     * nullable return explicitly.
     *
     * @param peerId The unique peer identifier to look up.
     * @return The matching [Peer] or `null`.
     */
    @Query("SELECT * FROM peers WHERE peerId = :peerId")
    suspend fun getPeerById(peerId: String): Peer?

    /**
     * Returns all peers whose current state is one of the given [states].
     *
     * Typical usage: filtering the peer list to show only peers in specific
     * states such as [PeerState.CONNECTED] or [PeerState.DISCOVERED].
     *
     * @param states A list of [PeerState] values to filter on.
     * @return [Flow] emitting the filtered peer list whenever it changes.
     */
    @Query("SELECT * FROM peers WHERE state IN (:states)")
    fun getPeersByStates(states: List<PeerState>): Flow<List<Peer>>

    /**
     * Looks up a peer by its BLE MAC address.
     *
     * Used during the BLE scanning pipeline to resolve a discovered MAC
     * address to an existing peer record before initiating a connection.
     *
     * @param address The BLE MAC address string to match.
     * @return The matching [Peer] or `null` if the address is unknown.
     */
    @Query("SELECT * FROM peers WHERE bleAddress = :address")
    suspend fun getPeerByAddress(address: String): Peer?

    /**
     * Inserts a peer or replaces the existing record if one with the same
     * primary key already exists.
     *
     * This upsert semantic is critical because peers are continuously updated
     * with fresh RSSI and timestamp data during BLE scans.
     *
     * @param peer The peer entity to insert or replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeer(peer: Peer)

    /**
     * Applies a partial update to an existing peer entity.
     *
     * Only the fields present in [peer] will be written; other columns retain
     * their current values. The update is matched on the primary key.
     *
     * @param peer The peer with updated field values.
     */
    @Update
    suspend fun updatePeer(peer: Peer)

    /**
     * Deletes a specific peer from the database.
     *
     * @param peer The peer entity to remove.
     */
    @Delete
    suspend fun deletePeer(peer: Peer)

    /**
     * Bulk-deletes all peers whose [Peer.lastSeenAt] is older than [threshold].
     *
     * This is invoked periodically by the mesh housekeeping routine to evict
     * peers that have dropped off the BLE mesh and are no longer reachable.
     *
     * @param threshold Epoch millis; peers last seen before this time are removed.
     */
    @Query("DELETE FROM peers WHERE lastSeenAt < :threshold")
    suspend fun deleteStalePeers(threshold: Long)

    /**
     * Updates only the RSSI reading and timestamp for a single peer without
     * touching any other columns.
     *
     * This is a high-frequency operation called on every BLE scan callback
     * that re-discovers the same peer, so it targets only the two columns
     * that change.
     *
     * @param peerId  The peer to update.
     * @param rssi    The latest signal-strength reading in dBm.
     * @param timestamp The epoch millis at which the reading was taken.
     */
    @Query("UPDATE peers SET rssi = :rssi, lastSeenAt = :timestamp WHERE peerId = :peerId")
    suspend fun updatePeerRssi(peerId: String, rssi: Int, timestamp: Long)

    /**
     * Returns the number of peers currently in the [PeerState.CONNECTED] state.
     *
     * Used to drive the connection-status indicator in the UI header. The
     * [Flow] re-emits whenever a peer's state changes.
     *
     * @return [Flow] emitting the live count of connected peers.
     */
    @Query("SELECT COUNT(*) FROM peers WHERE state = 'CONNECTED'")
    fun getConnectedPeerCount(): Flow<Int>

    /**
     * Irreversibly removes every row from the peers table.
     *
     * Typically called during account reset or sign-out. Callers should
     * confirm the action with the user before invoking this method.
     */
    @Query("DELETE FROM peers")
    suspend fun deleteAll()
}
