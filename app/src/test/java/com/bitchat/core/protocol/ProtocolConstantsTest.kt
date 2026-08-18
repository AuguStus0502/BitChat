package com.bitchat.core.protocol

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ProtocolConstants] to validate protocol boundaries.
 *
 * These tests ensure that the protocol constants are within expected ranges
 * and satisfy the security constraints documented in the class.
 */
class ProtocolConstantsTest {

    /**
     * Verify protocol version is positive and within expected range.
     * This prevents accidental regression to version 0.
     */
    @Test
    fun protocolVersion_isPositive() {
        assertTrue(
            "Protocol version must be at least 1",
            ProtocolConstants.PROTOCOL_VERSION >= 1
        )
    }

    /**
     * Verify MAX_PACKET_SIZE aligns with BLE MTU constraints.
     * The default Android BLE MTU negotiation target is 512 bytes.
     */
    @Test
    fun maxPacketSize_doesNotExceedBleMtu() {
        assertTrue(
            "MAX_PACKET_SIZE must not exceed 512 bytes (BLE MTU)",
            ProtocolConstants.MAX_PACKET_SIZE <= 512
        )
    }

    /**
     * Verify MAX_PAYLOAD_SIZE leaves sufficient headroom within MAX_PACKET_SIZE.
     * The difference must accommodate the serialized packet header and signature.
     */
    @Test
    fun maxPayloadSize_leavesHeadroomForHeader() {
        val headroom = ProtocolConstants.MAX_PACKET_SIZE - ProtocolConstants.MAX_PAYLOAD_SIZE
        assertTrue(
            "Headroom between MAX_PACKET_SIZE and MAX_PAYLOAD_SIZE must be at least 64 bytes " +
                    "for serialized header and signature fields",
            headroom >= 64
        )
    }

    /**
     * Verify TTL bounds prevent infinite packet propagation.
     */
    @Test
    fun maxTtl_isReasonable() {
        assertTrue(
            "MAX_TTL must be at least 1",
            ProtocolConstants.MAX_TTL >= 1
        )
        assertTrue(
            "MAX_TTL must not exceed 20 to prevent amplification attacks",
            ProtocolConstants.MAX_TTL <= 20
        )
    }

    /**
     * Verify DEFAULT_TTL is within the valid range.
     */
    @Test
    fun defaultTtl_isWithinValidRange() {
        assertTrue(
            "DEFAULT_TTL must be at least 1",
            ProtocolConstants.DEFAULT_TTL >= 1
        )
        assertTrue(
            "DEFAULT_TTL must not exceed MAX_TTL",
            ProtocolConstants.DEFAULT_TTL <= ProtocolConstants.MAX_TTL
        )
    }

    /**
     * Verify MAX_HOP_COUNT is at least as large as DEFAULT_TTL.
     * Otherwise, messages with default TTL would be dropped at hop 0.
     */
    @Test
    fun maxHopCount_exceedsDefaultTtl() {
        assertTrue(
            "MAX_HOP_COUNT must be at least DEFAULT_TTL",
            ProtocolConstants.MAX_HOP_COUNT >= ProtocolConstants.DEFAULT_TTL
        )
    }

    /**
     * Verify MAX_RETRY_COUNT is reasonable to prevent amplification attacks.
     */
    @Test
    fun maxRetryCount_isReasonable() {
        assertTrue(
            "MAX_RETRY_COUNT must be at least 1",
            ProtocolConstants.MAX_RETRY_COUNT >= 1
        )
        assertTrue(
            "MAX_RETRY_COUNT must not exceed 20",
            ProtocolConstants.MAX_RETRY_COUNT <= 20
        )
    }

    /**
     * Verify message expiry times are positive.
     */
    @Test
    fun messageExpiry_isPositive() {
        assertTrue(
            "DEFAULT_MESSAGE_EXPIRY_MS must be positive",
            ProtocolConstants.DEFAULT_MESSAGE_EXPIRY_MS > 0
        )
        assertTrue(
            "SOS_MESSAGE_EXPIRY_MS must be positive",
            ProtocolConstants.SOS_MESSAGE_EXPIRY_MS > 0
        )
    }

    /**
     * Verify SOS messages expire faster than regular messages.
     * Emergency relevance decays quickly.
     */
    @Test
    fun sosExpiry_isShorterThanDefault() {
        assertTrue(
            "SOS expiry must be shorter than default expiry (emergency relevance decays quickly)",
            ProtocolConstants.SOS_MESSAGE_EXPIRY_MS < ProtocolConstants.DEFAULT_MESSAGE_EXPIRY_MS
        )
    }

    /**
     * Verify heartbeat interval is reasonable for peer liveness detection.
     */
    @Test
    fun heartbeatInterval_isReasonable() {
        assertTrue(
            "Heartbeat interval must be at least 10 seconds",
            ProtocolConstants.HEARTBEAT_INTERVAL_MS >= 10_000L
        )
        assertTrue(
            "Heartbeat interval must not exceed 60 seconds",
            ProtocolConstants.HEARTBEAT_INTERVAL_MS <= 60_000L
        )
    }

    /**
     * Verify session expiry prevents long-lived session key exposure.
     */
    @Test
    fun sessionExpiry_isReasonable() {
        assertTrue(
            "Session expiry must be at least 5 minutes",
            ProtocolConstants.SESSION_EXPIRY_MS >= 5 * 60 * 1000L
        )
        assertTrue(
            "Session expiry must not exceed 24 hours",
            ProtocolConstants.SESSION_EXPIRY_MS <= 24 * 60 * 60 * 1000L
        )
    }

    /**
     * Verify MAX_PEERS is within reasonable device limits.
     */
    @Test
    fun maxPeers_isReasonable() {
        assertTrue(
            "MAX_PEERS must be at least 5",
            ProtocolConstants.MAX_PEERS >= 5
        )
        assertTrue(
            "MAX_PEERS must not exceed 200",
            ProtocolConstants.MAX_PEERS <= 200
        )
    }

    /**
     * Verify SEEN_MESSAGE_CACHE_SIZE is large enough to prevent relay loops.
     */
    @Test
    fun seenMessageCacheSize_isReasonable() {
        assertTrue(
            "SEEN_MESSAGE_CACHE_SIZE must be at least 100",
            ProtocolConstants.SEEN_MESSAGE_CACHE_SIZE >= 100
        )
    }

    /**
     * Verify all UUID strings are properly formatted.
     */
    @Test
    fun uuidStrings_areValidFormat() {
        val uuidPattern = Regex("^[0-9a-zA-Z-]+$")

        assertTrue(
            "SERVICE_UUID must be a valid UUID format (alphanumeric + hyphens)",
            uuidPattern.matches(ProtocolConstants.SERVICE_UUID)
        )
        assertTrue(
            "ADVERTISEMENT_UUID must be a valid UUID format (alphanumeric + hyphens)",
            uuidPattern.matches(ProtocolConstants.ADVERTISEMENT_UUID)
        )
        assertTrue(
            "CHARACTERISTIC_UUID must be a valid UUID format (alphanumeric + hyphens)",
            uuidPattern.matches(ProtocolConstants.CHARACTERISTIC_UUID)
        )
    }
}
