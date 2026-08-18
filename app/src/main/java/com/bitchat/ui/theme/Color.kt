package com.bitchat.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * BitChat Color Palette
 *
 * Design Philosophy:
 * BitChat is a humanitarian communication tool designed for crisis situations, disaster response,
 * and community resilience. The color palette reflects this mission through a calm, trustworthy,
 * and professional aesthetic — deliberately avoiding cyberpunk or aggressive visual language.
 *
 * Every color is chosen to evoke **reliability**, **warmth**, and **clarity** under stress.
 * Users in emergency scenarios need interfaces that feel stable and reassuring, not alarming.
 * The palette prioritizes legibility, calm emotional tone, and clear status communication.
 *
 * Accessibility Notes:
 * - All colors are chosen to meet WCAG 2.1 AA contrast ratios against their intended backgrounds.
 * - Emergency status colors (critical/help/stable) are paired with high-contrast light backgrounds
 *   to ensure visibility for users with color vision deficiencies.
 * - Semantic colors are never used in isolation — they are always accompanied by icons and text
 *   labels to provide redundant status communication.
 */

// =============================================================================
// Primary Colors — Calm Blue (Trust, Communication, Reliability)
// =============================================================================
// Blue is universally associated with trust, stability, and open communication.
// As the primary brand color, it anchors the UI in a feeling of calm competence.
// Used for primary actions, the app bar, and interactive focus states.

/** Core brand blue — used for primary buttons, links, and the status bar. */
val PrimaryBlue = Color(0xFF1565C0)

/** Lighter blue for containers, subtle highlights, and secondary emphasis. */
val PrimaryBlueLight = Color(0xFF5E92F3)

/** Darker blue for high-emphasis text on light containers and iconography. */
val PrimaryBlueDark = Color(0xFF003C8F)

// =============================================================================
// Secondary Colors — Warm Teal (Safety, Support, Community)
// =============================================================================
// Teal bridges blue's trustworthiness with a warmer, more human quality.
// It signals safety networks and mutual support without feeling clinical.
// Used sparingly for secondary actions and supportive UI elements.

/** Secondary brand teal — used for secondary buttons and accent surfaces. */
val SecondaryTeal = Color(0xFF00796B)

/** Lighter teal for container fills and subtle decorative elements. */
val SecondaryTealLight = Color(0xFF48A999)

// =============================================================================
// Tertiary Colors — Soft Amber (Warmth, Gentle Attention)
// =============================================================================
// Amber adds warmth and draws attention without the alarm of red or orange.
// It humanizes the interface, suggesting caution wrapped in care rather than danger.
// Used for warnings, highlights, and elements that need gentle visual emphasis.

/** Tertiary amber — used for caution states and warm accent elements. */
val TertiaryAmber = Color(0xFFF57F17)

/** Lighter amber for container fills and soft background highlights. */
val TertiaryAmberLight = Color(0xFFFFBF47)

// =============================================================================
// Emergency Status Colors (SOS / Crisis Communication)
// =============================================================================
// These colors communicate the severity of emergency situations. They follow a
// universally understood traffic-light metaphor:
//   Red    = Immediate danger (CRITICAL) — requires instant action
//   Orange = Urgent need (HELP_NEEDED) — requires attention soon
//   Green  = Stabilized (STABLE) — situation is under control
//
// Accessibility: Each status is always paired with a text label (CRITICAL/HELP/STABLE),
// an icon, and a distinct background tint — ensuring readability even for colorblind users.
// Background variants are intentionally very light (high luminance) to maximize contrast
// with the foreground status color.

/** Deep red for life-threatening or time-critical emergencies. */
val EmergencyCritical = Color(0xFFD32F2F)

/** Warm orange for situations requiring urgent help but not immediately life-threatening. */
val EmergencyHelpNeeded = Color(0xFFF57C00)

/** Calm green indicating the situation has stabilized or help has arrived. */
val EmergencyStable = Color(0xFF388E3C)

/** Very light red tint for CRITICAL status card/row backgrounds. */
val EmergencyCriticalBg = Color(0xFFFFEBEE)

/** Very light orange tint for HELP_NEEDED status card/row backgrounds. */
val EmergencyHelpBg = Color(0xFFFFF3E0)

/** Very light green tint for STABLE status card/row backgrounds. */
val EmergencyStableBg = Color(0xFFE8F5E9)

// =============================================================================
// Message Delivery Status Colors
// =============================================================================
// These communicate the lifecycle of a sent message in the mesh network.
// Colors progress from neutral (queued) → warm (sending) → green (delivered) or red (failed),
// giving users a quick visual sense of message reliability.

/** Green — message successfully delivered to the intended recipient(s). */
val StatusDelivered = Color(0xFF388E3C)

/** Orange — message is actively being transmitted or relayed through the mesh. */
val StatusSending = Color(0xFFF57C00)

/** Red — message delivery failed; user should retry or check connectivity. */
val StatusFailed = Color(0xFFD32F2F)

/** Neutral gray — message is queued and waiting for an available transmission path. */
val StatusQueued = Color(0xFF757575)

// =============================================================================
// Signal Strength Colors (Bluetooth Mesh Connectivity)
// =============================================================================
// BitChat relies on Bluetooth mesh networking. These colors communicate
// connection quality so users can make informed decisions about positioning
// and message timing during emergencies.

/** Green — strong, stable mesh connection; optimal for sending messages. */
val SignalStrong = Color(0xFF388E3C)

/** Orange — connection is recovering or fluctuating; send with caution. */
val SignalImproving = Color(0xFFF57C00)

/** Red — weak connection; messages may fail or require multiple relay hops. */
val SignalWeak = Color(0xFFE53935)

/** Gray — no active connection; user is offline from the mesh. */
val SignalLost = Color(0xFF9E9E9E)

// =============================================================================
// Background & Surface Colors
// =============================================================================
// The light, airy background palette creates a sense of openness and calm.
// Surfaces use near-white tones to maximize readability and reduce visual fatigue
// during prolonged use — important in extended crisis scenarios.

/** Near-white base background for the entire app. */
val BackgroundLight = Color(0xFFFAFBFC)

/** Pure white for elevated surfaces (cards, sheets, dialogs). */
val SurfaceLight = Color(0xFFFFFFFF)

/** Soft blue-gray tint for secondary surfaces and grouped content areas. */
val SurfaceVariantLight = Color(0xFFF0F4F8)

// =============================================================================
// Text Colors
// =============================================================================
// Text colors are designed for maximum legibility on the light background palette.
// The dark navy primary text avoids pure black (#000000) to reduce eye strain,
// while the secondary gray establishes clear visual hierarchy.

/** Dark navy for primary body text — high contrast without harshness. */
val TextPrimary = Color(0xFF1A2138)

/** Medium gray for secondary/supporting text, hints, and captions. */
val TextSecondary = Color(0xFF5F6B7A)

/** Pure white for text placed on colored surfaces (buttons, badges, status bars). */
val TextOnPrimary = Color(0xFFFFFFFF)
