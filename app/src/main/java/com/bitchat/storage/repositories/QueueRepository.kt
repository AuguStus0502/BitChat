package com.bitchat.storage.repositories

import com.bitchat.core.models.QueueItem
import com.bitchat.core.models.QueueStatus
import com.bitchat.storage.database.QueueDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository for the offline message queue.
 * Manages messages that cannot be delivered immediately, implementing
 * store-and-forward semantics with exponential backoff retry.
 */
class QueueRepository(private val queueDao: QueueDao) {

    /** Observe pending items for UI badge display. */
    fun observePendingCount(): Flow<Int> = queueDao.getPendingCount()

    /** Observe all pending items sorted by next retry time. */
    fun observePendingItems(): Flow<List<QueueItem>> = queueDao.getPendingItems()

    /** Enqueue a message for later delivery. */
    suspend fun enqueue(item: QueueItem) = queueDao.insertQueueItem(item)

    /** Get pending items for a specific destination (for relay decisions). */
    suspend fun getPendingForDestination(destinationId: String): List<QueueItem> =
        queueDao.getPendingForDestination(destinationId)

    /** Record a retry attempt with exponential backoff. */
    suspend fun recordRetry(queueId: String, backoffMs: Long) {
        val now = System.currentTimeMillis()
        val nextRetry = now + backoffMs
        queueDao.recordRetry(queueId, now, nextRetry)
    }

    /** Mark an item as delivered (terminal success state). */
    suspend fun markDelivered(queueId: String) =
        queueDao.updateStatus(queueId, QueueStatus.DELIVERED)

    /** Mark an item as permanently failed (terminal failure state). */
    suspend fun markFailed(queueId: String) =
        queueDao.updateStatus(queueId, QueueStatus.FAILED)

    /** Mark an item as currently being retried. */
    suspend fun markRetrying(queueId: String) =
        queueDao.updateStatus(queueId, QueueStatus.RETRYING)

    /** Clean up terminal-state items to prevent unbounded growth. */
    suspend fun cleanup() = queueDao.cleanupOldItems()

    /** Remove all queue items (used during panic wipe). */
    suspend fun clearAll() = queueDao.deleteAll()
}
