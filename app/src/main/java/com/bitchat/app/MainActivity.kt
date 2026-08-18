package com.bitchat.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import com.bitchat.ui.navigation.BitChatNavHost
import com.bitchat.ui.theme.BitChatTheme

/**
 * The single Activity hosting the entire Jetpack Compose UI.
 *
 * BitChat follows a single-Activity, Compose-only architecture. There are no
 * Fragments; all navigation is handled by [BitChatNavHost] through the
 * Compose Navigation library.
 *
 * ## Security & system-bars considerations
 *
 * * **Edge-to-edge** — [enableEdgeToEdge] draws the app content behind the
 *   status and navigation bars while respecting system gesture insets. This
 *   prevents content from being clipped behind translucent system chrome and
 *   gives a modern, immersive look without requiring the deprecated
 *   `SYSTEM_UI_FLAG` flags.
 * * **Theme setup** — The entire tree is wrapped in [BitChatTheme] so every
 *   child composable resolves Material 3 colours, typography, and shapes
 *   from a single source. The outermost [Surface] ensures the window
 *   background colour matches the theme, avoiding white flashes on
 *   configuration changes.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extend layout behind system bars for a seamless edge-to-edge look.
        enableEdgeToEdge()

        setContent {
            BitChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Root navigation host — all screens live inside this tree.
                    BitChatNavHost()
                }
            }
        }
    }
}
