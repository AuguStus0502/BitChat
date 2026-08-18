# BitChat Architecture

## Overview

BitChat uses a modular layered architecture for a decentralized Bluetooth P2P communication system.

## Layers

```
UI (Jetpack Compose)
    |
Presentation (ViewModel)
    |
Application Services
    |
Messaging Layer
    |
Routing Layer
    |
Security Layer
    |
Transport Abstraction
    |
BLE Transport / Nostr Transport
    |
Persistence (Room Database)
```

## Modules

### UI Layer
- `ui/screens` - Screen composables organized by feature
- `ui/components` - Shared UI components
- `ui/theme` - Material 3 theme configuration
- `ui/navigation` - Navigation graph and routes

### Core Layer
- `core/models` - Data models (Peer, Message, Identity, Session, etc.)
- `core/protocol` - BLE protocol definitions (Packet, PacketType, ProtocolConstants)
- `core/serialization` - Data serialization
- `core/utils` - Utility functions

### Network Layer
- `network/transport` - Transport abstraction
- `network/ble` - Bluetooth Low Energy implementation
- `network/discovery` - Peer discovery
- `network/connection` - Connection management
- `network/routing` - Multi-hop routing

### Security Layer
- `security/identity` - Identity generation and management
- `security/handshake` - Authenticated handshake protocol
- `security/keys` - Key management (Android Keystore)
- `security/encryption` - End-to-end encryption
- `security/authentication` - Peer authentication

### Storage Layer
- `storage/database` - Room database and DAOs
- `storage/repositories` - Data repositories
- `storage/queue` - Offline message queue
- `storage/preferences` - User preferences

### Diagnostics Layer
- `diagnostics/logging` - Event logging
- `diagnostics/metrics` - Performance metrics
- `diagnostics/experiments` - Experiment framework
- `diagnostics/export` - Data export (CSV/JSON)
