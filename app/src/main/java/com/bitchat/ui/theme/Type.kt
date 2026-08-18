package com.bitchat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * BitChat Typography System
 *
 * Design Philosophy:
 * Typography in BitChat prioritizes **clarity** and **readability** above all else.
 * In crisis situations, users may be reading under stress, in poor lighting, or while
 * moving. Every type size, weight, and spacing value is chosen to maximize legibility
 * and reduce cognitive load.
 *
 * The system uses the platform default font family to ensure consistent rendering
 * across all Android devices without requiring custom font bundling, which improves
 * app startup time and APK size — both critical when the app needs to install quickly
 * on a borrowed or emergency device.
 *
 * Hierarchy:
 * - Display/Headlines: Large, bold text for screen titles and emergency alerts
 * - Titles: Medium-emphasis text for section headers and navigation labels
 * - Body: Regular weight for message content and primary information
 * - Labels: Compact text for buttons, chips, timestamps, and metadata
 *
 * Accessibility Notes:
 * - Minimum body text size is 14sp (bodyMedium) to meet readability guidelines
 * - Letter spacing is tuned per style to prevent letter crowding at small sizes
 * - Line heights provide generous vertical rhythm for comfortable scanning
 */

val Typography = Typography(
    // -------------------------------------------------------------------------
    // Display — Largest text, used for hero sections or splash screens.
    // Normal weight keeps it approachable rather than shouty.
    // -------------------------------------------------------------------------
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),

    // -------------------------------------------------------------------------
    // Headlines — Primary screen titles and section headers.
    // SemiBold weight provides emphasis without feeling heavy.
    // -------------------------------------------------------------------------
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),

    // -------------------------------------------------------------------------
    // Titles — Sub-headers, dialog titles, list item titles.
    // Medium weight provides moderate emphasis for hierarchical structure.
    // -------------------------------------------------------------------------
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),

    // -------------------------------------------------------------------------
    // Body — Primary content text (messages, descriptions, body copy).
    // Normal weight with generous line height for extended reading comfort.
    // -------------------------------------------------------------------------
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // -------------------------------------------------------------------------
    // Labels — Buttons, chips, badges, timestamps, and metadata.
    // Medium weight ensures visibility at small sizes. Wider letter spacing
    // improves legibility for short, uppercase, or compact text strings.
    // -------------------------------------------------------------------------
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
