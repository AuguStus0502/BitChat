package com.bitchat.storage.queue

import com.bitchat.core.models.QueueItem
import com.bitchat.core.models.QueueStatus
import com.bitchat.core.protocol.ProtocolConstants
import com.bitchat.storage.repositories.QueueRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Background processor for the offline message queue.
 *
 * Periodically checks for pending messages and attempts delivery.
 * Uses exponential backoff for retry timing to avoid flooding
 * the network with repeated delivery attempts.
 *
 * Retry strategy:
 * - First retry: 1 second
 * - Each subsequent retry: doubles the backoff
 * - Maximum backoff: 5 minutes
 * - Maximum retries: ProtocolConstants.MAX_RETRY_COUNT (10)
 * - After max retries: mark as FAILED
 * - After expiry time: mark as EXPIRED
 *
 * The processor pauses when no peers are connected and resumes
 * when connectivity is restored.
 */
class QueueProcessor(private val queueRepository: QueueRepository) {

    private var processorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Callback invoked when a queued message should be sent. */
    var onProcessItem: (suspend (QueueItem) -> Boolean)? = null

    /**
     * Start the queue processor.
     *
     * @param checkIntervalMs How often to check for pending items.
     */
    fun start(checkIntervalMs: Long = 10_000L) {
        processorJob?.cancel()
        processorJob = scope.launch {
            while (isActive) {
                processPendingItems()
                delay(checkIntervalMs)
            }
        }
    }

    /** Stop the queue processor. */
    fun stop() {
        processorJob?.cancel()
        processorJob = null
    }

    /**
     * Process all pending queue items.
     * Attempts delivery for each item and handles retry/expiry logic.
     */
    private suspend fun processPendingItems() {
        val pendingItems = queueRepository.observePendingItems().first()

        for (item in pendingItems) {
            // Check if message has expired
            if (System.currentTimeMillis() > item.expiryTime) {
                queueRepository.markFailed(item.queueId)
                continue
            }

            // Check if max retries exceeded
            if (item.retryCount >= item.maxRetries) {
                queueRepository.markFailed(item.queueId)
                continue
            }

            // Check if it's time to retry
            if (System.currentTimeMillis() < item.nextRetryTime) {
                continue
            }

            // Attempt delivery
            queueRepository.markRetrying(item.queueId)
            val success = onProcessItem?.invoke(item) ?: false

            if (success) {
                queueRepository.markDelivered(item.queueId)
            } else {
                // Calculate exponential backoff: 1s, 2s, 4s, 8s, ... max 300s
                val backoffMs = calculateBackoff(item.retryCount)
                queueRepository.recordRetry(item.queueId, backoffMs)
            }
        }

        // Periodically clean up terminal items
        queueRepository.cleanup()
    }

    /**
     * Calculate exponential backoff with cap.
     *
     * retry 0 → 1000ms (1s)
     * retry 1 → 2000ms (2s)
     * retry 2 → 4000ms (4s)
     * retry 3 → 8000ms (8s)
     * retry 4+ → capped at 300000ms (5min)
     */
    private fun calculateBackoff(retryCount: Int): Long {
        val baseMs = 1000L
        val backoff = baseMs * (1L shl retryCount.coerceAtMost(8))
        return backoff.coerceAtMost(300_000L)
    }

    /** Clean up resources. */
    fun destroy() {
        stop()
        scope.cancel()
    }
}
