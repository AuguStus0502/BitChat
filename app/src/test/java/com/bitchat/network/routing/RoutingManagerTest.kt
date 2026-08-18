package com.bitchat.network.routing

import com.bitchat.core.protocol.Packet
import com.bitchat.core.protocol.PacketType
import com.bitchat.core.protocol.ProtocolConstants
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [RoutingManager] relay decision logic.
 *
 * Validates that the routing layer correctly:
 * - Drops own messages (loop prevention)
 * - Drops packets with exhausted TTL
 * - Drops duplicate messages (deduplication)
 * - Drops packets that exceed max hop count
 * - Correctly identifies packets destined for this node
 * - Makes correct relay/process/drop decisions
 */
class RoutingManagerTest {

    private lateinit var routingManager: RoutingManager

    @Before
    fun setUp() {
        routingManager = RoutingManager()
        routingManager.ourPeerId = "local_peer_001"
    }

    @Test
    fun evaluateRelay_ownMessage_isDropped() {
        val packet = createPacket(senderId = "local_peer_001")

        val decision = routingManager.evaluateRelay(packet, "sender_address")

        assertTrue(
            "Own messages must be dropped to prevent relay loops",
            decision is RelayDecision.Drop
        )
    }

    @Test
    fun evaluateRelay_ttlExhausted_isDropped() {
        val packet = createPacket(ttl = 0, senderId = "remote_peer_002")

        val decision = routingManager.evaluateRelay(packet, "sender_address")

        assertTrue(
            "Packets with TTL <= 0 must be dropped",
            decision is RelayDecision.Drop
        )
    }

    @Test
    fun evaluateRelay_duplicateMessage_isDropped() {
        val packet = createPacket(senderId = "remote_peer_002")

        // Process the packet once — this adds it to the seen cache
        routingManager.evaluateRelay(packet, "address_1")

        // Process the same packet again — should be dropped as duplicate
        val decision = routingManager.evaluateRelay(packet, "address_2")

        assertTrue(
            "Duplicate messages must be dropped",
            decision is RelayDecision.Drop
        )
    }

    @Test
    fun evaluateRelay_maxHopCountExceeded_isDropped() {
        val packet = createPacket(
            senderId = "remote_peer_002",
            hopCount = ProtocolConstants.MAX_HOP_COUNT
        )

        val decision = routingManager.evaluateRelay(packet, "sender_address")

        assertTrue(
            "Packets exceeding MAX_HOP_COUNT must be dropped",
            decision is RelayDecision.Drop
        )
    }

    @Test
    fun evaluateRelay_forUsWithTtlAbove1_processAndRelay() {
        val packet = createPacket(
            senderId = "remote_peer_002",
            destinationId = "local_peer_001",
            ttl = 3
        )

        val decision = routingManager.evaluateRelay(packet, "sender_address")

        assertTrue(
            "Packets for us with TTL > 1 should be processed AND relayed",
            decision is RelayDecision.ProcessAndRelay
        )
    }

    @Test
    fun evaluateRelay_forUsWithTtlOne_processOnly() {
        val packet = createPacket(
            senderId = "remote_peer_002",
            destinationId = "local_peer_001",
            ttl = 1
        )

        val decision = routingManager.evaluateRelay(packet, "sender_address")

        assertTrue(
            "Packets for us with TTL = 1 should be processed but NOT relayed",
            decision is RelayDecision.ProcessOnly
        )
    }

    @Test
    fun evaluateRelay_notForUsWithTtlAbove1_relayOnly() {
        val packet = createPacket(
            senderId = "remote_peer_002",
            destinationId = "other_peer_003",
            ttl = 3
        )

        val decision = routingManager.evaluateRelay(packet, "sender_address")

        assertTrue(
            "Packets not for us with TTL > 1 should be relayed only",
            decision is RelayDecision.RelayOnly
        )
    }

    @Test
    fun evaluateRelay_broadcastDestination_processAndRelay() {
        // null destinationId means broadcast to all
        val packet = createPacket(
            senderId = "remote_peer_002",
            destinationId = null,
            ttl = 3
        )

        val decision = routingManager.evaluateRelay(packet, "sender_address")

        assertTrue(
            "Broadcast packets (null destination) should be processed locally",
            decision is RelayDecision.ProcessAndRelay
        )
    }

    @Test
    fun createRelayPacket_decrementsTtlAndIncrementsHopCount() {
        val original = createPacket(
            senderId = "remote_peer_002",
            ttl = 5,
            hopCount = 2
        )

        val relayed = routingManager.createRelayPacket(original)

        assertEquals(
            "Relayed packet TTL should be decremented by 1",
            4, relayed.ttl
        )
        assertEquals(
            "Relayed packet hop count should be incremented by 1",
            3, relayed.hopCount
        )
        assertEquals(
            "Original sender ID must be preserved in relay",
            "remote_peer_002", relayed.senderId
        )
    }

    @Test
    fun clearCache_removesAllSeenMessageIds() {
        val packet = createPacket(senderId = "remote_peer_002")
        routingManager.evaluateRelay(packet, "address") // Adds to cache

        routingManager.clearCache()

        // After clearing, the same packet should not be detected as duplicate
        val decision = routingManager.evaluateRelay(packet, "address")
        assertTrue(
            "After cache clear, previously seen messages should be accepted",
            decision !is RelayDecision.Drop || decision.toString().contains("Duplicate").not()
        )
    }

    @Test
    fun createPacket_addsMessageIdToSeenCache() {
        routingManager.createPacket(
            message = com.bitchat.core.models.Message(
                messageId = "test_msg_123",
                senderId = "remote_peer_002",
                senderName = "Test",
                content = "Hello",
                type = com.bitchat.core.models.MessageType.DIRECT_MESSAGE
            ),
            messageType = PacketType.ENCRYPTED_MESSAGE
        )

        // The same message ID should now be detected as duplicate
        val packet2 = createPacket(
            messageId = "test_msg_123",
            senderId = "other_peer"
        )
        val decision = routingManager.evaluateRelay(packet2, "address")
        assertTrue(
            "Message ID added to seen cache via createPacket should be detected as duplicate",
            decision is RelayDecision.Drop
        )
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private fun createPacket(
        messageId: String = "test_${System.nanoTime()}",
        senderId: String = "remote_peer_002",
        destinationId: String? = null,
        ttl: Int = ProtocolConstants.DEFAULT_TTL,
        hopCount: Int = 0
    ): Packet {
        return Packet(
            messageType = PacketType.ENCRYPTED_MESSAGE,
            messageId = messageId,
            senderId = senderId,
            destinationId = destinationId,
            ttl = ttl,
            hopCount = hopCount
        )
    }
}
