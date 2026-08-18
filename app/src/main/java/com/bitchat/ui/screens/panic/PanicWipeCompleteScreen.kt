package com.bitchat.ui.screens.panic

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bitchat.ui.navigation.Screen
import com.bitchat.ui.theme.*

/**
 * Post-wipe confirmation screen shown after the emergency wipe completes.
 *
 * Displays a success indicator and a brief summary of what was erased,
 * then automatically navigates back to [Screen.Home] after a 3-second delay.
 *
 * ### User Journey Context
 * This is the terminal screen of the panic-wipe flow:
 * **[PanicWipeConfirmScreen] → wipe executes → [PanicWipeCompleteScreen] (this) → Home**.
 *
 * The back stack is cleared so the user cannot navigate back into the wipe
 * flow or the now-invalidated session state.
 *
 * ### Accessibility Notes
 * - The green checkmark icon ([EmergencyStable]) and "Session Cleared"
 *   heading provide clear, colour-independent confirmation (icon shape
 *   conveys success even for colour-blind users).
 * - Large 80dp icon and 28sp heading are easily readable.
 * - The auto-navigation message ("You will return to the setup screen")
 *   prepares the user for the upcoming transition, reducing confusion.
 *
 * @param navController Standard Jetpack Navigation controller; navigates
 *        to [Screen.Home] after the delay, clearing the back stack.
 */
@Composable
fun PanicWipeCompleteScreen(navController: NavController) {
    // ── Auto-Navigation Timer ─────────────────────────────────────────
    // After 3 seconds, navigate to Home and clear the entire back stack
    // so the user starts fresh with no prior session state.
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Home.route) { inclusive = true }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = EmergencyStable
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Session Cleared",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "All cryptographic keys, sessions, and private data have been permanently deleted.",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "You will return to the setup screen.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
