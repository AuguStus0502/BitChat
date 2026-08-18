# BitChat BLE Protocol

## Overview

BitChat uses a custom application-layer protocol over Bluetooth Low Energy for peer-to-peer communication.

## Packet Structure

Every packet includes:
- Protocol version (1 byte)
- Message type (1 byte)
- Message ID (UUID)
- Sender ID
- Destination ID
- Channel ID
- TTL
- Hop count
- Timestamp
- Payload
- Signature

## Message Types

| Type | Value | Description |
|------|-------|-------------|
| DISCOVERY | 0x01 | Peer discovery |
| CAPABILITIES | 0x02 | Feature advertisement |
| HANDSHAKE_INIT | 0x10 | Session initiation |
| HANDSHAKE_RESPONSE | 0x11 | Session response |
| HANDSHAKE_COMPLETE | 0x12 | Session established |
| ENCRYPTED_MESSAGE | 0x20 | Encrypted direct message |
| CHANNEL_MESSAGE | 0x21 | Channel broadcast |
| SOS_BROADCAST | 0x30 | Emergency SOS |
| SOS_RELAY | 0x31 | SOS forwarding |
| ACK | 0x40 | Delivery acknowledgement |
| RELAY | 0x50 | Message forwarding |
| QUEUE_SYNC | 0x60 | Queue synchronization |
| HEARTBEAT | 0x70 | Keepalive |
| ERROR | 0xF0 | Error notification |

## Handshake Protocol

Based on Noise Protocol Framework patterns:

1. Initiator sends HANDSHAKE_INIT with ephemeral public key
2. Responder sends HANDSHAKE_RESPONSE with ephemeral key and auth data
3. Both derive session key using X25519
4. User verifies visual/haptic pattern
5. HANDSHAKE_COMPLETE confirms mutual authentication
