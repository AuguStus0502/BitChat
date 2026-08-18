package com.bitchat.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

/**
 * Standalone onboarding screen dedicated to disaster-readiness messaging.
 *
 * Part of the initial setup journey: **Splash → Onboarding → Permissions → Home**.
 *
 * Currently a placeholder that will be expanded in a later phase with
 * detailed content about SOS relay mechanics, mesh resilience during
 * infrastructure failures, and emergency-communication best practices.
 *
 * ### Accessibility Notes
 * - Full-screen centred layout with large text ensures readability for
 *   users with low vision. Future iterations should add a large icon
 *   and a call-to-action button with a ≥48dp touch target.
 *
 * @param navController Standard Jetpack Navigation controller.
 */
@Composable
fun OnboardingDisasterScreen(@Suppress("UNUSED_PARAMETER") navController: NavController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Disaster Ready Onboarding")
    }
}
