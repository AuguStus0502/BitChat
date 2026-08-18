package com.bitchat.storage.database

import androidx.room.*
import com.bitchat.core.models.QueueItem
import com.bitchat.core.models.QueueStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing [QueueItem] entities in the local Room database.
 *
 * The message queue is a durable, retry-capable outbound pipeline. When a
 * message cannot be delivered immediately (e.g., the destination peer is not
 * reachable over BLE), it is persisted here and retried with exponential
 * back-off until it succeeds, expires, or is permanently marked as failed.
 *
 * ## Threading model
 * - **Reactive queries** (returning [Flow]) feed the queue-monitoring UI
 *   (pending count, pending-item list) and are collected on a background
 *   coroutine.
 * - **Suspend functions** handle queue mutations on Room's write dispatcher
 *   and are safe to call from any coroutine scope.
 *
 * ## Retry semantics
 * Each [QueueItem] tracks a [QueueItem.retryCount], [QueueItem.lastRetryTime],
 * and [QueueItem.nextRetryTime]. The dispatcher selects items ordered by
 * [QueueItem.nextRetryTime] ascending (soonest first). After each attempt,
 * [recordRetry] atomically increments the retry counter and computes the
 * next back-off window.
 *
 * ## Lifecycle
 * Items that reach [QueueStatus.EXPIRED] or [QueueStatus.FAILED] are
 * purged by [cleanupOldItems] to prevent unbounded table growth.
 */
@Dao
interface QueueDao {

    /**
     * Returns all items whose status is PENDING or RETRYING, ordered by
     * [QueueItem.nextRetryTime] ascending (soonest first).
     *
     * This is the primary feed for the background delivery worker, which
     * polls the queue and dispatches the most urgent items first. The
     * [Flow] re-emits on every status change, insert, or deletion.
     *
     * @return [Flow] emitting the ordered list of deliverable queue items.
     */
    @Query("SELECT * FROM message_queue WHERE status = 'PENDING' OR status = 'RETRYING' ORDER BY nextRetryTime ASC")
    fun getPendingItems(): Flow<List<QueueItem>>

    /**
     * Fetches a single queue item by its unique identifier.
     *
     * @param queueId The queue item's primary key.
     * @return The matching [QueueItem] or `null`.
     */
    @Query("SELECT * FROM message_queue WHERE queueId = :queueId")
    suspend fun getQueueItemById(queueId: String): QueueItem?

    /**
     * Returns all PENDING or RETRYING items destined for a specific peer.
     *
     * Useful for deduplication checks or for flushing the queue for a
     * particular peer when a connection is established.
     *
     * @param destinationId The destination peer's identifier.
     * @return A list of pending [QueueItem] records (may be empty).
     */
    @Query("SELECT * FROM message_queue WHERE destinationId = :destinationId AND (status = 'PENDING' OR status = 'RETRYING')")
    suspend fun getPendingForDestination(destinationId: String): List<QueueItem>

    /**
     * Inserts a queue item or replaces the existing record when one with the
     * same primary key already exists.
     *
     * @param item The queue item to insert or replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueueItem(item: QueueItem)

    /**
     * Applies a partial update to an existing queue item.
     *
     * @param item The queue item with updated field values.
     */
    @Update
    suspend fun updateQueueItem(item: QueueItem)

    /**
     * Deletes a specific queue item from the database.
     *
     * @param item The queue item to remove.
     */
    @Delete
    suspend fun deleteQueueItem(item: QueueItem)

    /**
     * Updates only the status of a single queue item.
     *
     * Called when an item transitions between pipeline states (e.g.,
     * PENDING -> RETRYING, or RETRYING -> DELIVERED).
     *
     * @param queueId The queue item to update.
     * @param status  The new [QueueStatus] value.
     */
    @Query("UPDATE message_queue SET status = :status WHERE queueId = :queueId")
    suspend fun updateStatus(queueId: String, status: QueueStatus)

    /**
     * Atomically increments the retry counter and records the current and
     * next-retry timestamps for a single queue item.
     *
     * This is called after each failed delivery attempt. The back-off
     * interval (`nextRetry - now`) is computed by the caller before
     * invoking this method.
     *
     * @param queueId   The queue item to update.
     * @param now       Epoch millis of the current (failed) attempt.
     * @param nextRetry Epoch millis when the next retry should be scheduled.
     */
    @Query("UPDATE message_queue SET retryCount = retryCount + 1, lastRetryTime = :now, nextRetryTime = :nextRetry WHERE queueId = :queueId")
    suspend fun recordRetry(queueId: String, now: Long, nextRetry: Long)

    /**
     * Deletes all queue items whose status is EXPIRED or FAILED.
     *
     * These items are terminal — they will never be retried — and are
     * purged to reclaim storage. This method should be called periodically
     * (e.g., once per session or on a background timer).
     */
    @Query("DELETE FROM message_queue WHERE status = 'EXPIRED' OR status = 'FAILED'")
    suspend fun cleanupOldItems()

    /**
     * Irreversibly removes every item from the queue.
     *
     * Used during account reset. Active deliveries will fail on their next
     * retry and should be gracefully handled by the dispatcher.
     */
    @Query("DELETE FROM message_queue")
    suspend fun deleteAll()

    /**
     * Returns the number of items currently eligible for delivery.
     *
     * Used to display a queue-depth badge or progress indicator in the UI.
     * The [Flow] re-emits whenever the pending/ retrying count changes.
     *
     * @return [Flow] emitting the live count of deliverable queue items.
     */
    @Query("SELECT COUNT(*) FROM message_queue WHERE status = 'PENDING' OR status = 'RETRYING'")
    fun getPendingCount(): Flow<Int>
}
