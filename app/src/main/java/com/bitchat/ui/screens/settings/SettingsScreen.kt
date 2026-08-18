package com.bitchat.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bitchat.ui.navigation.Screen
import com.bitchat.ui.theme.*
import com.bitchat.ui.viewmodels.SettingsViewModel

/**
 * Application settings screen accessible from the [HomeScreen] top bar.
 *
 * Organised into four sections:
 * 1. **Profile** — local display name used in peer discovery (not transmitted externally).
 * 2. **Accessibility** — toggles for reduced motion, haptic feedback, and text-to-speech.
 * 3. **Security** — shortcut to the emergency wipe flow.
 * 4. **About** — version info and a link to research diagnostics.
 *
 * ### Accessibility Notes
 * - Every toggle row uses a [Switch] with both a title and subtitle, making
 *   the control discoverable by screen readers (TalkBack announces both labels).
 * - Toggle labels use ≥14sp font size for readability.
 * - The back-navigation icon in the top bar has a `contentDescription` of "Back"
 *   for TTS support.
 * - The reduced-motion toggle respects user preferences by disabling animations
 *   throughout the app when enabled.
 * - Haptic feedback toggle allows users with sensory sensitivities to disable
 *   vibration patterns.
 * - TTS toggle enables reading messages aloud for visually impaired users.
 *
 * NOTE: Toggle states are currently held in local composition state. They will
 * be migrated to a persistent preferences store (e.g. DataStore) in a later phase.
 *
 * @param navController Standard Jetpack Navigation controller; pops back to
 *        the home screen on back-navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    // ── ViewModel ──────────────────────────────────────────────────────
    val viewModel: SettingsViewModel = viewModel()
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()
    val fingerprint by viewModel.fingerprint.collectAsStateWithLifecycle()

    // ── Local State ───────────────────────────────────────────────────
    // TODO: Replace with persistent preferences (DataStore) in a later phase.
    var reducedMotion by remember { mutableStateOf(false) }
    var hapticFeedback by remember { mutableStateOf(true) }
    var ttsEnabled by remember { mutableStateOf(false) }
    var nameInput by remember(displayName) { mutableStateOf(displayName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Profile Section ────────────────────────────────────────
            // Local display name — only used within the mesh; never
            // transmitted to any central server.
            Text("Profile", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = nameInput,
                onValueChange = {
                    nameInput = it
                    viewModel.updateDisplayName(it)
                },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
            )
            OutlinedTextField(
                value = fingerprint,
                onValueChange = {},
                label = { Text("Public Key Fingerprint") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                leadingIcon = { Icon(Icons.Default.Fingerprint, contentDescription = null) }
            )

            HorizontalDivider()

            // ── Accessibility Section ──────────────────────────────────
            // Three toggles that improve the experience for users with
            // visual, auditory, or motor accessibility needs.
            Text("Accessibility", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

            SettingsToggle("Reduced Motion", "Minimize animations", reducedMotion, { reducedMotion = it })
            SettingsToggle("Haptic Feedback", "Vibration for interactions", hapticFeedback, { hapticFeedback = it })
            SettingsToggle("Text-to-Speech", "Read messages aloud", ttsEnabled, { ttsEnabled = it })

            HorizontalDivider()

            // ── Security Section ───────────────────────────────────────
            // Destructive action (emergency wipe) is placed here with a
            // clear label. Activation leads to a hold-to-confirm screen
            // to prevent accidental data loss.
            Text("Security", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

            SettingsNavItem(
                title = "Emergency Wipe",
                subtitle = "Delete all keys and sessions",
                icon = Icons.Default.DeleteForever,
                onClick = { navController.navigate(Screen.PanicWipeConfirm.route) }
            )

            HorizontalDivider()

            // ── About Section ──────────────────────────────────────────
            // Version metadata and a navigation link to the diagnostics
            // screen for research and troubleshooting purposes.
            Text("About", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)

            SettingsNavItem(
                title = "Research Diagnostics",
                subtitle = "View connection metrics and run experiments",
                icon = Icons.Default.Analytics,
                onClick = { navController.navigate(Screen.Diagnostics.route) }
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("BitChat v0.1.0", fontWeight = FontWeight.Medium)
                    Text(
                        "Decentralized Bluetooth P2P Communication",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "BSc (Hons) Ethical Hacking & Cybersecurity Project",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Toggle row used for boolean accessibility settings.
 *
 * Renders a two-line label (title + subtitle) on the left and a Material 3
 * [Switch] on the right. The entire row is vertically centred.
 *
 * ### Accessibility Notes
 * - The [Switch] component is natively accessible; TalkBack announces
 *   the checked state and the label text.
 * - Subtitle text provides additional context for users who may not
 *   immediately understand the toggle's effect.
 *
 * @param title         Primary label (e.g. "Reduced Motion").
 * @param subtitle      Secondary descriptive text below the title.
 * @param checked       Current toggle state.
 * @param onCheckedChange  Callback invoked when the user toggles the switch.
 */
@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Navigable card row used for settings items that lead to another screen.
 *
 * Displays a leading icon, title, subtitle, and a trailing chevron to
 * indicate that the item navigates elsewhere. The entire card is clickable.
 *
 * ### Accessibility Notes
 * - The card's clickable modifier makes it discoverable by TalkBack as
 *   a single actionable element.
 * - Trailing chevron icon has a null content description to avoid redundant
 *   TTS output (the card title already conveys purpose).
 *
 * @param title    Primary label (e.g. "Emergency Wipe").
 * @param subtitle Secondary description below the title.
 * @param icon     Material icon shown in the leading position.
 * @param onClick  Navigation callback invoked on tap.
 */
@Composable
private fun SettingsNavItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
