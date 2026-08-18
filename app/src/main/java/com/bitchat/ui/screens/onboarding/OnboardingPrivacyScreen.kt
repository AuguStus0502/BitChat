package com.bitchat.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

/**
 * Standalone onboarding screen dedicated to the privacy-by-design message.
 *
 * Part of the initial setup journey: **Splash → Onboarding → Permissions → Home**.
 *
 * Currently a placeholder that will be expanded in a later phase with
 * content about end-to-end encryption, local-only identity storage,
 * zero-knowledge architecture, and the absence of central servers.
 *
 * ### Accessibility Notes
 * - Full-screen centred layout provides a simple, distraction-free reading
 *   experience. Future iterations should include a prominent lock icon
 *   and a navigation button meeting the ≥48dp touch-target guideline.
 *
 * @param navController Standard Jetpack Navigation controller.
 */
@Composable
fun OnboardingPrivacyScreen(@Suppress("UNUSED_PARAMETER") navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Private by Design Onboarding")
    }
}
