package com.bitchat.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bitchat.ui.navigation.Screen
import com.bitchat.ui.theme.PrimaryBlue
import kotlinx.coroutines.delay

/**
 * Animated splash screen displayed on cold start.
 *
 * This is the very first screen in the application's user journey:
 * **Splash → Onboarding → Permissions → Home**.
 *
 * Shows the BitChat logo and tagline with a fade-in animation, holds for
 * a brief duration to allow branding impression, then automatically
 * navigates to the onboarding flow ([Screen.OnboardingOffline]).
 *
 * The splash route is popped from the back stack so the user cannot
 * navigate back to it once the transition completes.
 *
 * ### Accessibility Notes
 * - The icon carries a `contentDescription` of "BitChat" for TalkBack/TTS.
 * - The screen is purely decorative/informational; no interactive elements
 *   are present, so no additional accessibility actions are needed.
 *
 * @param navController Standard Jetpack Navigation controller; navigates
 *        to [Screen.OnboardingOffline] after the splash animation.
 */
@Composable
fun SplashScreen(navController: NavController) {
    // Fade-in animation: starts fully transparent (0f), animates to fully opaque (1f)
    val alpha = remember { Animatable(0f) }

    // Trigger the fade-in, hold, then navigate to onboarding
    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        delay(1500)
        navController.navigate(Screen.OnboardingOffline.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    // Full-screen primary-coloured background with centred branding
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha.value)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "BitChat",
                modifier = Modifier.size(80.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "BitChat",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Offline Mesh Communication",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
