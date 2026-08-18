package com.bitchat.core.protocol

/**
 * Central repository of protocol-level constants for the BitChat BLE mesh network.
 *
 * All magic numbers, limits, timeouts, and BLE identifiers used across the protocol
 * stack are defined here to ensure a single source of truth and to make tuning
 * without code changes straightforward.
 *
 * ### Design Considerations
 * - **BLE MTU constraint**: The default Android BLE MTU negotiation target is 512
 *   bytes. [MAX_PACKET_SIZE] is set to match this so that each packet fits in a
 *   single ATT write operation without requiring L2CAP fragmentation.
 * - **Security boundary**: [MAX_TTL] and [MAX_HOP_COUNT] bound the total network
 *   footprint of any single message, limiting amplification attacks and broadcast
 *   storms in adversarial scenarios.
 * - **Resource limits**: [MAX_PEERS] and [SEEN_MESSAGE_CACHE_SIZE] cap memory
 *   usage on resource-constrained mobile devices.
 */
object ProtocolConstants {

    // ─── Protocol Versioning ────────────────────────────────────────────

    /**
     * Current protocol version identifier. Embedded in every [Packet.version] field.
     *
     * Increment this value when the wire format, serialization schema, or semantic
     * meaning of fields changes in a backward-incompatible way. Receivers MUST
     * reject packets with an unrecognized version to prevent silent corruption.
     */
    const val PROTOCOL_VERSION = 1

    // ─── Packet Size Limits ─────────────────────────────────────────────

    /**
     * Maximum serialized size of a [Packet] in bytes.
     *
     * Set to 512 to align with the typical Android BLE MTU after negotiation.
     * A packet exceeding this size will fail to transmit over a single BLE write
     * and must be rejected by the sender rather than relying on fragmentation.
     */
    const val MAX_PACKET_SIZE = 512

    /**
     * Maximum size of the [Packet.payload] field in bytes.
     *
     * Set to 440 to leave sufficient headroom (~72 bytes) for the serialized
     * packet header, metadata fields, and signature within [MAX_PACKET_SIZE].
     * Application code MUST truncate or chunk payloads that exceed this limit.
     */
    const val MAX_PAYLOAD_SIZE = 440

    // ─── TTL & Hop Limits ──────────────────────────────────────────────

    /**
     * Hard upper bound on the [Packet.ttl] field.
     *
     * **Security**: Capping TTL prevents a malicious or misconfigured node from
     * injecting a packet that propagates indefinitely through the mesh. This
     * bounds the total number of relay hops and the associated bandwidth cost.
     */
    const val MAX_TTL = 10

    /**
     * Default time-to-live assigned to newly created packets.
     *
     * A value of 5 allows messages to traverse up to 5 relay hops, which is
     * sufficient for most local-area mesh scenarios while limiting unnecessary
     * propagation.
     */
    const val DEFAULT_TTL = 5

    /**
     * Maximum hop count before a packet is considered unroutable and dropped.
     *
     * This acts as a safety net independent of TTL: even if TTL is misconfigured,
     * no packet will be forwarded more than this many times. Dropping occurs when
     * `hopCount >= MAX_HOP_COUNT`.
     */
    const val MAX_HOP_COUNT = 10

    // ─── Retry & Retry Limits ───────────────────────────────────────────

    /**
     * Maximum number of transmission retry attempts before a message is declared
     * undeliverable.
     *
     * **Security**: A low retry ceiling limits the amplification factor of
     * retransmission-based denial-of-service. Each retry still respects the
     * relay TTL budget.
     */
    const val MAX_RETRY_COUNT = 10

    // ─── Message Expiry ─────────────────────────────────────────────────

    /**
     * Default message expiry duration in milliseconds (24 hours).
     *
     * Messages older than this are discarded and no longer eligible for relay
     * or delivery. This prevents stale messages from consuming cache space
     * indefinitely and ensures the network eventually purges orphaned data.
     */
    const val DEFAULT_MESSAGE_EXPIRY_MS = 24 * 60 * 60 * 1000L

    /**
     * Expiry duration for SOS/emergency messages in milliseconds (6 hours).
     *
     * SOS messages use a shorter expiry than normal messages because emergency
     * relevance decays quickly. A 6-hour window balances timely delivery with
     * eventual cleanup.
     */
    const val SOS_MESSAGE_EXPIRY_MS = 6 * 60 * 60 * 1000L

    // ─── Timing & Intervals ────────────────────────────────────────────

    /**
     * Interval between periodic [PacketType.HEARTBEAT] transmissions in milliseconds.
     *
     * Peers that have not sent a heartbeat within `3 × HEARTBEAT_INTERVAL_MS`
     * are considered offline. A 30-second interval balances responsiveness
     * (fast failure detection) against BLE radio duty-cycle and battery impact.
     */
    const val HEARTBEAT_INTERVAL_MS = 30_000L

    /**
     * Duration after which an authenticated session is considered stale and
     * must be re-established via a new handshake, in milliseconds (30 minutes).
     *
     * **Security**: Session expiry limits the window during which a compromised
     * session key can be exploited. Periodic re-keying also refreshes the
     * forward-secrecy guarantee of the ECDH exchange.
     */
    const val SESSION_EXPIRY_MS = 30 * 60 * 1000L

    /**
     * Delay before attempting reconnection to a lost BLE peer, in milliseconds.
     *
     * A 5-second back-off prevents tight reconnection loops that would drain
     * battery while still re-establishing connectivity within a reasonable time.
     */
    const val RECONNECT_DELAY_MS = 5_000L

    // ─── Capacity Limits ───────────────────────────────────────────────

    /**
     * Maximum number of directly connected peer sessions maintained simultaneously.
     *
     * **Resource**: Each session consumes memory for key material, message queues,
     * and connection state. A cap of 50 is appropriate for typical consumer devices;
     * exceeded peers are evicted on a least-recently-used basis.
     */
    const val MAX_PEERS = 50

    /**
     * Capacity of the deduplication / seen-message cache.
     *
     * Each relay node stores the [Packet.messageId] of recently processed packets
     * in an LRU cache of this size to prevent duplicate processing and forwarding.
     * A cache of 1000 entries is sufficient for typical message rates over the
     * default 24-hour expiry window.
     */
    const val SEEN_MESSAGE_CACHE_SIZE = 1000

    // ─── BLE Service & Characteristic UUIDs ─────────────────────────────

    /**
     * Base GATT service UUID for BitChat.
     *
     * This UUID is advertised during BLE scanning and must match the service
     * defined in the peripheral's GATT server. Central devices filter scan
     * results on this UUID to identify BitChat peers.
     */
    const val SERVICE_UUID = "0000bitchat-0000-1000-8000-00805f9b34fb"

    /**
     * GATT characteristic UUID used for BLE advertisement data exchange.
     *
     * During the discovery phase, peers read this characteristic to obtain
     * the advertiser's identity and capabilities without establishing a
     * full GATT connection.
     */
    const val ADVERTISEMENT_UUID = "0000bitchat-advt-0000-1000-8000-00805f9b34fb"

    /**
     * GATT characteristic UUID used for serialized [Packet] transmission.
     *
     * All protocol packets (handshake, encrypted messages, heartbeats, etc.)
     * are written to and notified from this characteristic. The characteristic
     * uses [android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE]
     * for throughput and [android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY]
     * for incoming data.
     */
    const val CHARACTERISTIC_UUID = "0000bitchat-msgs-0000-1000-8000-00805f9b34fb"
}
