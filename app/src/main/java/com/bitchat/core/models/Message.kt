package com.bitchat.core.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Core message model for the BitChat mesh network.
 *
 * Represents any unit of data transmitted between peers, including user-authored messages,
 * protocol control packets (handshakes, heartbeats, ACKs), and relay envelopes. Messages
 * are persisted locally for offline queuing and delivery tracking.
 *
 * Delivery semantics:
 * - A [direct message][MessageType.DIRECT_MESSAGE] targets a specific [destinationId].
 * - A [channel message][MessageType.CHANNEL_MESSAGE] is broadcast to a named [channelName].
 * - [ttl] limits how many relay hops a message may traverse before being dropped.
 * - Messages with a non-null [expiryTime] are discarded after that timestamp regardless of TTL.
 *
 * @property messageId Globally unique identifier for this message.
 * @property senderId Peer ID of the originating device.
 * @property senderName Display name of the sender at time of transmission.
 * @property destinationId Target peer ID for direct messages; null for channel broadcasts.
 * @property channelName Target channel name for channel messages; null for direct messages.
 * @property content The message payload as a UTF-8 string.
 * @property type Categorizes the message for routing and handling logic.
 * @property priority Determines transmission ordering and BLE scheduling weight.
 * @property timestamp Creation timestamp (ms since epoch) used for ordering and display.
 * @property ttl Maximum number of relay hops remaining; decremented at each hop.
 * @property hopCount Number of hops this message has already traversed.
 * @property relayedBy Peer ID of the node that last relayed this message; null if sent directly.
 * @property status Delivery state tracked by the sending device.
 * @property isEncrypted Whether the [content] payload is encrypted end-to-end.
 * @property expiryTime Absolute timestamp (ms) after which this message is considered expired.
 * @property retryCount Number of delivery attempts so far.
 * @property lastRetryTime Timestamp (ms) of the most recent delivery retry.
 */
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val messageId: String,
    val senderId: String,
    val senderName: String,
    val destinationId: String? = null,
    val channelName: String? = null,
    val content: String,
    val type: MessageType,
    val priority: MessagePriority = MessagePriority.NORMAL,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = 5,
    val hopCount: Int = 0,
    val relayedBy: String? = null,
    val status: MessageStatus = MessageStatus.QUEUED,
    val isEncrypted: Boolean = false,
    val expiryTime: Long? = null,
    val retryCount: Int = 0,
    val lastRetryTime: Long? = null
)

/**
 * Categorizes a [Message] for routing, handling, and display purposes.
 */
enum class MessageType {
    /** A 1-to-1 message addressed to a specific peer. */
    DIRECT_MESSAGE,

    /** A broadcast message sent to all subscribers of a named channel. */
    CHANNEL_MESSAGE,

    /** An emergency distress beacon (see [SosBeacon]). */
    SOS_MESSAGE,

    /** A wrapper envelope used when one peer relays a message on behalf of another. */
    RELAY_MESSAGE,

    /** Acknowledgment that a previously sent message was received. */
    ACK,

    /** Cryptographic key-exchange handshake initiating a new session. */
    HANDSHAKE,

    /** Periodic signal indicating a peer is still alive and reachable. */
    HEARTBEAT,

    /** Synchronizes pending message queues between directly connected peers. */
    QUEUE_SYNC,

    /** Reports a delivery or protocol error to the sender. */
    ERROR
}

/**
 * Transmission priority level that influences BLE scheduling and relay selection.
 *
 * Higher priority messages are transmitted sooner and may preempt lower-priority traffic.
 */
enum class MessagePriority {
    /** Standard user message; transmitted in normal order. */
    NORMAL,

    /** Time-sensitive message that should be relayed ahead of NORMAL traffic. */
    HIGH,

    /** Emergency or system-critical message that preempts all other traffic. */
    CRITICAL
}

/**
 * Delivery status of a [Message] from the sending peer's perspective.
 *
 * Tracks the message through its lifecycle from local queuing to confirmed delivery
 * or terminal failure/expiry.
 */
enum class MessageStatus {
    /** Queued locally, waiting for an available BLE connection. */
    QUEUED,

    /** Actively being transmitted over BLE. */
    SENDING,

    /** Forwarded to a relay peer; awaiting relay-level confirmation. */
    RELAYING,

    /** Delivery confirmed by the destination peer or a subsequent relay. */
    DELIVERED,

    /** All delivery and retry attempts have been exhausted. */
    FAILED,

    /** Message exceeded its [Message.expiryTime] or TTL before delivery. */
    EXPIRED
}
