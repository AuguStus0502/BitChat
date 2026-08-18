package com.bitchat.ui.navigation

/**
 * Defines every navigable destination in the BitChat app as a type-safe route string.
 *
 * The app uses Jetpack Compose Navigation with a single-activity architecture.
 * Each [Screen] is a sealed class entry that maps to a unique route string consumed
 * by [BitChatNavHost]. Parameterised routes (e.g. [ActiveBeacon], [EphemeralChat])
 * expose a [createRoute] helper so callers never have to interpolate raw strings.
 *
 * ## Navigation flow
 *
 * ```
 * Splash -> OnboardingOffline -> OnboardingDisaster -> OnboardingPrivacy -> Permissions -> Home
 *                                                                                      |
 *                                               +------------------------------------+--+
 *                                               |              |            |           |
 *                                          SosComposer   PeerDiscovery  Settings  PanicWipeConfirm
 *                                               |              |            |           |
 *                                          SosConfirmation  ContactToken Diagnostics PanicWipeComplete
 *                                               |
 *                                         ActiveBeacon -> NearbySosFeed -> SosDetail
 *                                             
 *                                         HandshakeVerification -> EphemeralChat
 * ```
 *
 * Security note: no route carries sensitive user data in the URL path. Identifiers
 * are opaque beacons/peer IDs that are meaningless outside the current session.
 */
sealed class Screen(val route: String) {

    // ── First-launch flow ──────────────────────────────────────────────────

    /** Splash / loading screen shown while the app initialises. */
    data object Splash : Screen("splash")

    /** First onboarding slide — explains BitChat's offline-mesh concept. */
    data object OnboardingOffline : Screen("onboarding_offline")

    /** Second onboarding slide — highlights disaster-resilience features. */
    data object OnboardingDisaster : Screen("onboarding_disaster")

    /** Third onboarding slide — details the privacy / encryption model. */
    data object OnboardingPrivacy : Screen("onboarding_privacy")

    /** Runtime-permission request screen (BLE, location, notifications). */
    data object Permissions : Screen("permissions")

    // ── Core hub ───────────────────────────────────────────────────────────

    /** Main hub screen — dashboard with tabs for SOS, peers, and settings. */
    data object Home : Screen("home")

    // ── Emergency / SOS flow ───────────────────────────────────────────────

    /** Composer where the user drafts an SOS distress message. */
    data object SosComposer : Screen("sos_composer")

    /** Confirmation screen shown after an SOS beacon is broadcast. */
    data object SosConfirmation : Screen("sos_confirmation")

    /**
     * Live view of an actively-broadcasting SOS beacon.
     *
     * @param beaconId unique identifier of the beacon, embedded in the route.
     */
    data object ActiveBeacon : Screen("active_beacon/{beaconId}") {
        fun createRoute(beaconId: String) = "active_beacon/$beaconId"
    }

    /** Feed of nearby SOS beacons received over the BLE mesh. */
    data object NearbySosFeed : Screen("nearby_sos_feed")

    /**
     * Detail view for a single received SOS beacon.
     *
     * @param beaconId identifier of the beacon to display.
     */
    data object SosDetail : Screen("sos_detail/{beaconId}") {
        fun createRoute(beaconId: String) = "sos_detail/$beaconId"
    }

    // ── Private-messaging flow ─────────────────────────────────────────────

    /** BLE peer-scanning screen — discovers nearby BitChat users. */
    data object PeerDiscovery : Screen("peer_discovery")

    /** Displays the user's one-time contact token / QR code for exchange. */
    data object ContactToken : Screen("contact_token")

    /**
     * Manual or NFC-assisted handshake verification with a specific peer.
     *
     * @param peerId public fingerprint of the peer being verified.
     */
    data object HandshakeVerification : Screen("handshake_verification/{peerId}") {
        fun createRoute(peerId: String) = "handshake_verification/$peerId"
    }

    /**
     * End-to-end encrypted ephemeral chat session.
     *
     * @param peerId public fingerprint of the conversation partner.
     */
    data object EphemeralChat : Screen("ephemeral_chat/{peerId}") {
        fun createRoute(peerId: String) = "ephemeral_chat/$peerId"
    }

    // ── Panic wipe flow ────────────────────────────────────────────────────

    /** Final confirmation dialog before erasing all local data. */
    data object PanicWipeConfirm : Screen("panic_wipe_confirm")

    /** Acknowledgement shown after a successful panic wipe. */
    data object PanicWipeComplete : Screen("panic_wipe_complete")

    // ── Utility ────────────────────────────────────────────────────────────

    /** App settings (identity, theme, network, about). */
    data object Settings : Screen("settings")

    /** Diagnostic / debug screen (logs, BLE state, DB stats). */
    data object Diagnostics : Screen("diagnostics")
}
