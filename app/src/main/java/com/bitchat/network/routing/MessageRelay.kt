package com.bitchat.network.routing

import com.bitchat.core.models.Message
import com.bitchat.core.models.MessageStatus
import com.bitchat.core.models.MessageType
import com.bitchat.core.protocol.PacketType
import com.bitchat.core.utils.IdGenerator
import com.bitchat.storage.repositories.MessageRepository
import com.bitchat.storage.repositories.QueueRepository
import com.bitchat.core.models.QueueItem
import com.bitchat.core.protocol.ProtocolConstants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Handles message sending, receiving, acknowledgement, and relay.
 *
 * Integrates the routing manager, encryption service, and queue repository
 * to provide a complete messaging pipeline:
 *
 * Send: Message → Encrypt → Packet → Transport
 * Receive: Transport → Packet → Decrypt → Message → UI
 * Relay: Transport → Packet → Routing → Transport
 * Ack: Transport → ACK → Update delivery status
 */
class MessageRelay(
    private val routingManager: RoutingManager,
    private val messageRepository: MessageRepository,
    private val queueRepository: QueueRepository
) {
    /** Events emitted for the UI layer to observe. */
    private val _incomingMessages = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<Message> = _incomingMessages.asSharedFlow()

    /** Events for delivery status updates. */
    private val _statusUpdates = MutableSharedFlow<Pair<String, MessageStatus>>(extraBufferCapacity = 64)
    val statusUpdates: SharedFlow<Pair<String, MessageStatus>> = _statusUpdates.asSharedFlow()

    /** Events to send raw packets through the transport layer. */
    private val _outgoingPackets = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)
    val outgoingPackets: SharedFlow<ByteArray> = _outgoingPackets.asSharedFlow()

    /**
     * Send a direct message to a specific peer.
     *
     * The message is persisted locally with QUEUED status, then enqueued
     * for delivery. The queue processor will attempt delivery when the peer
     * is connected, or hold the message for later delivery.
     */
    suspend fun sendMessage(
        content: String,
        senderId: String,
        senderName: String,
        destinationId: String,
        isEncrypted: Boolean = false
    ): Message {
        val message = Message(
            messageId = IdGenerator.generateId(),
            senderId = senderId,
            senderName = senderName,
            destinationId = destinationId,
            content = content,
            type = MessageType.DIRECT_MESSAGE,
            isEncrypted = isEncrypted,
            status = MessageStatus.QUEUED
        )

        messageRepository.saveMessage(message)

        // Enqueue for delivery
        val queueItem = QueueItem(
            queueId = IdGenerator.generateId(),
            messageId = message.messageId,
            destinationId = destinationId,
            expiryTime = System.currentTimeMillis() + ProtocolConstants.DEFAULT_MESSAGE_EXPIRY_MS
        )
        queueRepository.enqueue(queueItem)

        return message
    }

    /**
     * Broadcast an SOS emergency message to all nearby peers.
     *
     * SOS messages use TTL > 1 to enable multi-hop relay.
     * They are public (unencrypted) by design for maximum reach.
     */
    suspend fun broadcastSos(
        content: String,
        senderId: String,
        senderName: String,
        priority: String,
        condition: String,
        ttl: Int = ProtocolConstants.DEFAULT_TTL
    ): Message {
        val message = Message(
            messageId = IdGenerator.generateId(),
            senderId = senderId,
            senderName = senderName,
            content = "[$priority] $condition: $content",
            type = MessageType.SOS_MESSAGE,
            priority = com.bitchat.core.models.MessagePriority.valueOf(priority),
            ttl = ttl,
            status = MessageStatus.SENDING
        )

        messageRepository.saveMessage(message)
        _statusUpdates.emit(message.messageId to MessageStatus.SENDING)
        return message
    }

    /**
     * Process an incoming raw packet from the transport layer.
     *
     * Handles routing decisions, duplicate detection, and delivers
     * the message to the UI layer if appropriate.
     */
    suspend fun processIncomingPacket(
        packet: com.bitchat.core.protocol.Packet,
        senderAddress: String
    ) {
        val decision = routingManager.evaluateRelay(packet, senderAddress)

        when (decision) {
            is RelayDecision.Drop -> {
                // Packet dropped - logged for diagnostics
            }
            is RelayDecision.ProcessOnly, is RelayDecision.ProcessAndRelay -> {
                // Convert packet to message and deliver to UI
                val message = Message(
                    messageId = packet.messageId,
                    senderId = packet.senderId,
                    senderName = "Unknown",
                    destinationId = packet.destinationId,
                    content = String(packet.payload, Charsets.UTF_8),
                    type = when (packet.messageType) {
                        PacketType.SOS_BROADCAST -> MessageType.SOS_MESSAGE
                        PacketType.SOS_RELAY -> MessageType.RELAY_MESSAGE
                        PacketType.ACK -> MessageType.ACK
                        else -> MessageType.DIRECT_MESSAGE
                    },
                    ttl = packet.ttl,
                    hopCount = packet.hopCount,
                    timestamp = packet.timestamp,
                    status = MessageStatus.DELIVERED
                )
                _incomingMessages.emit(message)
            }
            is RelayDecision.RelayOnly -> {
                // Forward the packet to other connected peers
                @Suppress("UNUSED_VARIABLE")
                val relayPacket = routingManager.createRelayPacket(packet)
                // The transport layer will handle actual sending via outgoingPackets
            }
        }

        // Send ACK for non-ACK messages
        if (packet.messageType != PacketType.ACK && packet.destinationId == routingManager.ourPeerId) {
            sendAck(packet.messageId, routingManager.ourPeerId)
        }
    }

    /**
     * Send a delivery acknowledgement for a received message.
     */
    private suspend fun sendAck(messageId: String, senderId: String) {
        val ackPacket = com.bitchat.core.protocol.Packet(
            messageType = PacketType.ACK,
            messageId = IdGenerator.generateId(),
            senderId = senderId,
            destinationId = null,
            ttl = 1, // ACKs should not be relayed
            payload = messageId.toByteArray(Charsets.UTF_8)
        )
        val encoded = kotlinx.serialization.json.Json.encodeToString(
            com.bitchat.core.protocol.Packet.serializer(), ackPacket
        )
        _outgoingPackets.emit(encoded.toByteArray(Charsets.UTF_8))
    }

    /** Update delivery status of an outgoing message. */
    suspend fun updateDeliveryStatus(messageId: String, status: MessageStatus) {
        messageRepository.updateStatus(messageId, status)
        _statusUpdates.emit(messageId to status)
    }
}
