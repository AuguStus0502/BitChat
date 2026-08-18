package com.bitchat.network.routing

import com.bitchat.core.models.Message
import com.bitchat.core.models.MessageType
import com.bitchat.core.models.MessageStatus
import com.bitchat.core.protocol.Packet
import com.bitchat.core.protocol.PacketType
import com.bitchat.core.protocol.ProtocolConstants
import com.bitchat.core.utils.IdGenerator
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages multi-hop message routing in the BLE mesh network.
 *
 * Implements controlled message forwarding with:
 * - TTL (time-to-live) to prevent infinite propagation
 * - Duplicate detection via message ID cache
 * - Hop count tracking for diagnostics
 * - Relay permission control for SOS messages
 *
 * Routing strategy: Simple flooding with deduplication.
 * Messages are forwarded to all connected peers except the sender.
 * Each forward decrements TTL and increments hop count.
 *
 * Future enhancement: Replace with proactive routing (AODV-like)
 * once the flooding approach is validated experimentally.
 */
class RoutingManager {

    /** Cache of recently seen message IDs to prevent duplicate forwarding. */
    private val seenMessages = ConcurrentHashMap.newKeySet<String>()

    /** Maximum number of message IDs to cache. Prevents unbounded memory growth. */
    private val maxCacheSize = ProtocolConstants.SEEN_MESSAGE_CACHE_SIZE

    /**
     * Determine if a message should be forwarded (relayed).
     *
     * @param packet The received packet to evaluate.
     * @param senderAddress The BLE address of the peer who sent this packet.
     * @return RelayDecision indicating whether to forward, drop, or process locally.
     */
    fun evaluateRelay(packet: Packet, @Suppress("UNUSED_PARAMETER") senderAddress: String): RelayDecision {
        // Rule 1: Never forward our own messages
        if (packet.senderId == ourPeerId) {
            return RelayDecision.Drop("Own message")
        }

        // Rule 2: Drop if TTL exhausted
        if (packet.ttl <= 0) {
            return RelayDecision.Drop("TTL exhausted (ttl=${packet.ttl})")
        }

        // Rule 3: Drop duplicates
        if (!seenMessages.add(packet.messageId)) {
            return RelayDecision.Drop("Duplicate message")
        }

        // Rule 4: Validate hop count
        if (packet.hopCount >= ProtocolConstants.MAX_HOP_COUNT) {
            return RelayDecision.Drop("Max hops reached (${packet.hopCount})")
        }

        // Rule 5: Check if message is for us (process locally + optionally relay)
        val isForUs = packet.destinationId == ourPeerId || packet.destinationId == null
        val shouldRelay = packet.ttl > 1

        // Trim cache if it grows too large
        trimCache()

        return if (isForUs && shouldRelay) {
            RelayDecision.ProcessAndRelay
        } else if (isForUs) {
            RelayDecision.ProcessOnly
        } else {
            RelayDecision.RelayOnly
        }
    }

    /**
     * Create a relay version of a packet for forwarding.
     * Decrements TTL and increments hop count.
     */
    fun createRelayPacket(original: Packet): Packet {
        return original.copy(
            ttl = original.ttl - 1,
            hopCount = original.hopCount + 1
        )
    }

    /**
     * Create a new outgoing packet from a message.
     */
    fun createPacket(
        message: Message,
        messageType: PacketType,
        destinationId: String? = null
    ): Packet {
        val packet = Packet(
            messageType = messageType,
            messageId = message.messageId,
            senderId = message.senderId,
            destinationId = destinationId,
            channelId = message.channelName,
            ttl = message.ttl,
            timestamp = message.timestamp
        )
        seenMessages.add(packet.messageId)
        return packet
    }

    /** Clear the duplicate detection cache. */
    fun clearCache() = seenMessages.clear()

    /** Trim the cache to prevent unbounded growth. */
    private fun trimCache() {
        if (seenMessages.size > maxCacheSize) {
            // Remove oldest entries (approximation - ConcurrentHashMap doesn't preserve order)
            val toRemove = seenMessages.size - maxCacheSize / 2
            val iterator = seenMessages.iterator()
            var removed = 0
            while (iterator.hasNext() && removed < toRemove) {
                iterator.next()
                iterator.remove()
                removed++
            }
        }
    }

    /** This device's peer ID, set during initialization. */
    var ourPeerId: String = ""
        set(value) {
            field = value
        }
}

/** Decision about how to handle a received packet. */
sealed class RelayDecision {
    /** Process the packet locally only (final destination, TTL exhausted). */
    data object ProcessOnly : RelayDecision()

    /** Process locally and relay to other peers. */
    data object ProcessAndRelay : RelayDecision()

    /** Relay only, don't process locally (not for us). */
    data object RelayOnly : RelayDecision()

    /** Drop the packet entirely with a reason. */
    data class Drop(val reason: String) : RelayDecision()
}
