package com.bitchat.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A message delivery task in the outbound transmission queue.
 *
 * When a [Message] cannot be delivered immediately (e.g., no BLE connection to the destination),
 * a [QueueItem] is created to track the retry lifecycle. The queue manager polls pending items
 * and attempts delivery when a suitable connection becomes available.
 *
 * Retry behavior:
 * - Failed deliveries are retried with exponential backoff governed by [backoffMs].
 * - After [maxRetries] attempts the item transitions to [QueueStatus.FAILED].
 * - Items past their [expiryTime] are transitioned to [QueueStatus.EXPIRED].
 * - [nextRetryTime] determines when the queue manager should next attempt delivery.
 *
 * @property queueId Unique identifier for this queue entry (Room primary key).
 * @property messageId Reference to the [Message] being delivered.
 * @property destinationId Target peer ID that should receive the message.
 * @property createdAt Timestamp (ms) when this queue item was created.
 * @property ttl Maximum relay hops for the underlying message.
 * @property retryCount Number of delivery attempts made so far.
 * @property maxRetries Maximum allowed attempts before marking the item as failed.
 * @property lastRetryTime Timestamp (ms) of the most recent delivery attempt; null if never retried.
 * @property nextRetryTime Timestamp (ms) at which the next delivery attempt should be made.
 * @property status Current delivery state of this queue entry.
 * @property expiryTime Absolute timestamp (ms) after which this item is considered expired.
 * @property backoffMs Current backoff interval in milliseconds applied before the next retry.
 */
@Entity(tableName = "message_queue")
data class QueueItem(
    @PrimaryKey val queueId: String,
    val messageId: String,
    val destinationId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val ttl: Int = 5,
    val retryCount: Int = 0,
    val maxRetries: Int = 10,
    val lastRetryTime: Long? = null,
    val nextRetryTime: Long = System.currentTimeMillis(),
    val status: QueueStatus = QueueStatus.PENDING,
    val expiryTime: Long,
    val backoffMs: Long = 1000
)

/**
 * Delivery state of a [QueueItem] within the outbound message queue.
 */
enum class QueueStatus {
    /** Waiting for an available BLE connection to the destination peer. */
    PENDING,

    /** A delivery attempt failed; the item is waiting for its backoff period to elapse. */
    RETRYING,

    /** Destination peer has acknowledged receipt of the message. */
    DELIVERED,

    /** All retry attempts have been exhausted without successful delivery. */
    FAILED,

    /** The item exceeded its [QueueItem.expiryTime] before it could be delivered. */
    EXPIRED
}
