# BitChat

**A Decentralized Bluetooth P2P Communication System for Disaster Resilience and Privacy Sovereignty in Nepal**

BSc Final Year Project — ST6047CEM Cyber Security Project

---

## Overview

BitChat is a decentralized, offline-first communication application built for Android that enables peer-to-peer messaging over Bluetooth Low Energy (BLE). Designed for disaster scenarios where traditional network infrastructure is unavailable, BitChat allows users to discover nearby peers, exchange encrypted messages, and broadcast emergency SOS signals — all without internet or cellular connectivity.

---

## Features

- **Decentralized P2P Messaging** — Direct device-to-device communication over BLE with no server dependency
- **Encrypted Chat Sessions** — Ephemeral, end-to-end encrypted conversations using XChaCha20-Poly1305
- **SOS Broadcasting** — Broadcast and receive emergency SOS signals to/from nearby devices
- **Peer Discovery** — Automatic discovery and handshake verification of nearby BitChat users
- **Contact Token Exchange** — Secure exchange of contact identifiers via BLE
- **Authenticated Handshake** — Cryptographic handshake protocol to verify peer identity
- **Panic Wipe** — Instant destruction of all local data in emergency situations
- **Material 3 UI** — Modern Jetpack Compose interface with full accessibility support
- **Offline-First** — Fully functional without internet, Wi-Fi, or cellular connectivity
- **Privacy by Design** — No accounts, no cloud storage, no persistent identifiers

---

## Prerequisites

| Requirement | Version |
|---|---|
| JDK | 17 |
| Android SDK | API 34 |
| Android SDK Build Tools | 34.0.0 |
| Gradle | 8.5 (bundled via wrapper) |
| IDE | VS Code with Kotlin extension **or** Android Studio |

---

## Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/BitChat.git
   cd BitChat
   ```

2. **Configure the Android SDK**
   - Set the `ANDROID_HOME` environment variable to your Android SDK path, **or**
   - Create `local.properties` in the project root with:
     ```properties
     sdk.dir=C\:\\Users\\<your-username>\\AppData\\Local\\Android\\Sdk
     ```

3. **Set JAVA_HOME** to your JDK 17 installation directory.

4. **Build the debug APK**
   ```bash
   ./gradlew assembleDebug
   ```

5. **Locate the output APK**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

6. **Install on a connected device**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Build Commands

```bash
# Full debug build
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean assembleDebug
```

---

## Architecture

BitChat follows a layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────┐
│              UI Layer                       │
│         (Jetpack Compose / M3)              │
├─────────────────────────────────────────────┤
│           Presentation Layer                │
│            (ViewModels)                     │
├─────────────────────────────────────────────┤
│              Core Layer                     │
│         (Models, Protocol Logic)            │
├─────────────────────────────────────────────┤
│            Network Layer                    │
│        (BLE Communication)                  │
├─────────────────────────────────────────────┤
│           Security Layer                    │
│    (Encryption, Identity, Keystore)         │
├─────────────────────────────────────────────┤
│           Storage Layer                     │
│            (Room DB)                        │
└─────────────────────────────────────────────┘
```

**Technology Stack:**

| Layer | Technology |
|---|---|
| UI | Kotlin, Jetpack Compose, Material 3 |
| Presentation | Android ViewModel, Coroutines/Flow |
| Network | Android BLE APIs |
| Storage | Room Database |
| Security | Google Tink (XChaCha20-Poly1305), Android Keystore |

---

## Screens

BitChat contains **19 screens**:

| # | Screen | Purpose |
|---|---|---|
| 1 | Splash | App initialization and branding |
| 2 | Onboarding 1 | Feature introduction |
| 3 | Onboarding 2 | Feature introduction |
| 4 | Onboarding 3 | Feature introduction |
| 5 | Permissions | BLE and location permission requests |
| 6 | Home | Main dashboard and navigation hub |
| 7 | SOS Composer | Compose and configure an SOS broadcast |
| 8 | SOS Confirmation | Confirm before broadcasting SOS |
| 9 | Active Beacon | View own active SOS beacon status |
| 10 | Nearby SOS Feed | See SOS signals from nearby peers |
| 11 | SOS Detail | Full details of a received SOS |
| 12 | Peer Discovery | Scan and list nearby BitChat users |
| 13 | Contact Token | Exchange contact identifiers |
| 14 | Handshake Verification | Cryptographic peer verification |
| 15 | Ephemeral Chat | End-to-end encrypted messaging session |
| 16 | Panic Wipe Confirm | Confirm destructive data wipe |
| 17 | Panic Wipe Complete | Wipe confirmation screen |
| 18 | Settings | App configuration and diagnostics |
| 19 | Research Diagnostics | Development and research metrics |

---

## Security Model

BitChat implements a multi-layered security architecture:

- **XChaCha20-Poly1305 Encryption** — All message payloads are encrypted using Google Tink's implementation of the XChaCha20-Poly1305 AEAD cipher, providing authenticated encryption with associated data
- **Android Keystore** — Cryptographic keys are hardware-backed via the Android Keystore system, preventing extraction even on rooted devices
- **Authenticated Handshake** — A cryptographic handshake protocol verifies peer identity before any session is established
- **Ephemeral Sessions** — Chat sessions are temporary and leave no persistent trace unless explicitly retained
- **Panic Wipe** — A single-action destroy mechanism that irreversibly deletes all local data, keys, and session state
- **No Accounts or Cloud** — No user registration, no server-side storage, no third-party data exposure

---

## Testing

| Test Type | Status |
|---|---|
| Unit Tests | Planned (Phase 2) |
| Integration Tests | Planned (Phase 2) |
| Security Tests | Planned (Phase 3) |
| Performance Tests | Planned (Phase 3) |

---

## Development Phases

| Phase | Focus | Deliverables |
|---|---|---|
| **Phase 1** | Core Architecture | Project setup, BLE foundation, encryption, Room database |
| **Phase 2** | Feature Implementation | Full screen implementation, messaging, SOS, peer discovery |
| **Phase 3** | Security Hardening | Penetration testing, security audit, performance optimization |
| **Phase 4** | Research & Write-up | Diagnostics, metrics collection, final report documentation |

---

## License

This project is a BSc final year academic project. Contact the authors for licensing and usage terms.
