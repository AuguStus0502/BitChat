# Threat Model

## Overview

This document describes the security threats considered in BitChat's design.

## Threats

### 1. Impersonation
- **Attack Surface**: BLE advertising, identity exchange
- **Mitigation**: Cryptographic identity verification, signed handshakes
- **Residual Risk**: Short window during initial discovery

### 2. Replay Attacks
- **Attack Surface**: BLE transport
- **Mitigation**: Nonces, timestamps, message IDs, replay cache
- **Residual Risk**: None with proper implementation

### 3. Man-in-the-Middle
- **Attack Surface**: BLE connection, handshake
- **Mitigation**: Authenticated handshake, user verification of patterns
- **Residual Risk**: User must verify pattern correctly

### 4. Message Modification
- **Attack Surface**: BLE transport, relay
- **Mitigation**: AEAD encryption (XChaCha20-Poly1305)
- **Residual Risk**: None with proper implementation

### 5. Malicious Relay
- **Attack Surface**: Multi-hop forwarding
- **Mitigation**: End-to-end encryption, TTL limits
- **Residual Risk**: Relay can observe metadata (timing, size)

### 6. Traffic Analysis
- **Attack Surface**: BLE advertising and data exchange
- **Mitigation**: Limited by short-range BLE, no central server
- **Residual Risk**: Difficult to fully mitigate in mesh networks

### 7. Compromised Device
- **Attack Surface**: Local storage, keys
- **Mitigation**: Panic wipe, ephemeral sessions, Android Keystore
- **Residual Risk**: Device owner has full access

### 8. Denial of Service
- **Attack Surface**: BLE scanning, connection
- **Mitigation**: Rate limiting, connection limits, TTL
- **Residual Risk**: Local DoS possible
