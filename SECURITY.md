# Security Policy

## Overview

BitChat implements a decentralized, privacy-focused communication system. This document describes our security model and reporting process.

## Security Model

### Identity
- Cryptographic key pairs generated locally on device
- Private keys never leave the device
- No central identity server
- Temporary/local identity for peer discovery

### Communication
- End-to-end encryption using XChaCha20-Poly1305 via Google Tink
- Authenticated handshake using Noise Protocol patterns
- No central message server
- Bluetooth Low Energy as primary transport

### Data Storage
- All data stored locally using Room database
- Sensitive data protected by Android Keystore
- Panic wipe removes cryptographic keys and session data

## Known Limitations

- BLE transport is limited by physical proximity
- Multi-hop relay messages are visible to relay nodes (encrypted but metadata exposed)
- No perfect forward secrecy guaranteed across session reconnections
- Secure deletion of all physical storage remnants cannot be absolutely guaranteed

## Reporting a Vulnerability

If you discover a security vulnerability, please report it responsibly:

1. Do NOT create a public GitHub issue
2. Email the project team with details
3. Allow reasonable time for a fix before public disclosure

## Threat Model

The following threats are considered in the design:
- Impersonation
- Replay attacks
- Man-in-the-middle
- Message modification
- Malicious relay nodes
- Traffic analysis
- Metadata exposure
- Compromised/stolen device
- Denial of service
- Flooding
- Malformed packets

See `/docs/security/threat-model.md` for detailed analysis.
