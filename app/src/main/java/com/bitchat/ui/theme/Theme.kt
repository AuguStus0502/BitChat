package com.bitchat.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * BitChat Material 3 Theme
 *
 * Design Philosophy:
 * BitChat uses a light-only color scheme to maintain maximum readability and a calm,
 * professional appearance in all conditions — including bright outdoor environments
 * common during disaster and field scenarios. The theme avoids dark mode at this time
 * because:
 *
 * 1. Emergency responders often work outdoors in bright daylight
 * 2. A consistent light theme reduces cognitive overhead during high-stress moments
 * 3. Light backgrounds provide the highest contrast for status-critical information
 *
 * The color scheme maps BitChat's semantic color tokens onto Material 3's color role
 * system, ensuring compatibility with standard Material components while preserving
 * the humanitarian design intent.
 *
 * Status bar integration:
 * The status bar is tinted with the primary blue and uses light-on-dark iconography,
 * creating a cohesive visual boundary that frames the app content.
 */

/**
 * The Light color scheme for BitChat.
 *
 * Maps semantic color tokens to Material 3 color roles:
 * - **Primary**: Calm blue for primary actions and emphasis
 * - **Secondary**: Warm teal for supportive/secondary actions
 * - **Tertiary**: Soft amber for gentle attention and warnings
 * - **Error**: Emergency red for destructive actions and critical states
 * - **Background/Surface**: Near-white tones for calm, open reading environments
 * - **Containers**: Tinted versions of primary/secondary/tertiary at 30% opacity
 *   for card backgrounds and surface accents that maintain color identity without
 *   overwhelming the content they contain
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryBlueLight.copy(alpha = 0.3f),
    onPrimaryContainer = PrimaryBlueDark,
    secondary = SecondaryTeal,
    onSecondary = TextOnPrimary,
    secondaryContainer = SecondaryTealLight.copy(alpha = 0.3f),
    tertiary = TertiaryAmber,
    onTertiary = TextOnPrimary,
    tertiaryContainer = TertiaryAmberLight.copy(alpha = 0.3f),
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondary,
    error = EmergencyCritical,
    onError = TextOnPrimary
)

/**
 * The root composable that applies BitChat's visual identity to the entire UI tree.
 *
 * This function wraps [content] in a [MaterialTheme] with BitChat's custom color scheme
 * and typography. It also synchronizes the system status bar appearance with the
 * primary brand color for a cohesive visual experience.
 *
 * Usage:
 * ```kotlin
 * BitChatTheme {
 *     MyAppContent()
 * }
 * ```
 *
 * @param content The composable UI tree that will receive the BitChat theme configuration.
 */
@Composable
fun BitChatTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current

    // Apply status bar styling only outside of Android Studio previews.
    // The SideEffect runs after every successful recomposition to ensure
    // the status bar stays synchronized with the current color scheme.
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            // Use dark (light-on-dark) status bar icons to contrast against
            // the primary blue status bar background.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
