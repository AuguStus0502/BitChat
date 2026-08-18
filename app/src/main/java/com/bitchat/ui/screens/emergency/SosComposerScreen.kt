package com.bitchat.ui.screens.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.bitchat.core.models.SosPriority
import com.bitchat.ui.navigation.Screen
import com.bitchat.ui.theme.*
import com.bitchat.ui.viewmodels.SosViewModel

/**
 * First step of the emergency SOS flow where the user composes their distress message.
 *
 * This screen collects all necessary information before broadcasting an SOS beacon
 * over the BitChat mesh network. The disaster-resilience flow is:
 *
 * 1. **SosComposerScreen** (this screen) — user fills in priority, condition, message, and preferences.
 * 2. [SosConfirmationScreen] — user reviews a summary before final broadcast.
 * 3. [ActiveBeaconScreen] — live beacon status displayed while the SOS is active.
 *
 * Form data is passed forward to the confirmation screen via the NavController's
 * `savedStateHandle` on the current back stack entry, avoiding any need for a
 * shared ViewModel across these destinations.
 *
 * @param navController Used to navigate back or forward to [Screen.SosConfirmation].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosComposerScreen(navController: NavController, viewModel: SosViewModel = viewModel()) {

    // ── Form state from ViewModel ───────────────────────────────────────
    val selectedPriority by viewModel.priority.collectAsStateWithLifecycle()
    val condition by viewModel.condition.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val includeLocation by viewModel.includeLocation.collectAsStateWithLifecycle()
    val allowRelay by viewModel.allowRelay.collectAsStateWithLifecycle()

    /** Preset condition tags shown in the dropdown. */
    val conditions = listOf("Injured", "Trapped", "Need water", "Need food", "Need medical", "Safe but stranded", "Lost", "Other")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SOS Composer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Public warning banner ───────────────────────────────────
            // Informs the user that SOS messages are visible to every nearby
            // BitChat node. This is a critical transparency requirement for
            // disaster-resilient communication — users must understand the
            // broadcast scope before sending.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = EmergencyHelpBg
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = EmergencyHelpNeeded
                    )
                    Text(
                        text = "SOS messages are public emergency messages visible to all nearby BitChat users.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Priority selection ──────────────────────────────────────
            // Lets the sender classify urgency (CRITICAL / HELP_NEEDED /
            // STABLE) so nearby peers can triage responses accordingly.
            Text("Priority", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SosPriority.entries.forEach { priority ->
                    val (color, label) = when (priority) {
                        SosPriority.CRITICAL -> Pair(EmergencyCritical, "Critical")
                        SosPriority.HELP_NEEDED -> Pair(EmergencyHelpNeeded, "Help Needed")
                        SosPriority.STABLE -> Pair(EmergencyStable, "Stable")
                    }
                    val isSelected = selectedPriority == priority

                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setPriority(priority) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f),
                            selectedLabelColor = color
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Condition dropdown ──────────────────────────────────────
            // Structured condition tag chosen from a preset list, enabling
            // consistent filtering and search on the receiving end.
            Text("Condition", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

            var conditionExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = conditionExpanded,
                onExpandedChange = { conditionExpanded = it }
            ) {
                OutlinedTextField(
                    value = condition,
                    onValueChange = { viewModel.setCondition(it) },
                    label = { Text("Select condition") },
                    readOnly = condition.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = conditionExpanded,
                    onDismissRequest = { conditionExpanded = false }
                ) {
                    conditions.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c) },
                            onClick = {
                                viewModel.setCondition(c)
                                conditionExpanded = false
                            }
                        )
                    }
                }
            }

            // ── Free-form message ───────────────────────────────────────
            // Optional field for additional context beyond the structured
            // condition tag (e.g. specific injuries, number of people).
            OutlinedTextField(
                value = message,
                onValueChange = { viewModel.setMessage(it) },
                label = { Text("Additional details (optional)") },
                placeholder = { Text("Brief description of your situation") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            // ── Location toggle ─────────────────────────────────────────
            // When enabled, the device's GPS coordinates are attached to the
            // beacon so rescuers can narrow the search area.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Include approximate location", fontWeight = FontWeight.Medium)
                    Text(
                        "Uses device GPS if available",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = includeLocation,
                    onCheckedChange = { viewModel.setIncludeLocation(it) }
                )
            }

            // ── Relay toggle ────────────────────────────────────────────
            // Allows other BitChat nodes to forward this SOS further across
            // the mesh, extending reach when direct contact is unavailable.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Allow relay forwarding", fontWeight = FontWeight.Medium)
                    Text(
                        "Other devices can relay your SOS",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = allowRelay,
                    onCheckedChange = { viewModel.setAllowRelay(it) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Submit button ───────────────────────────────────────────
            // Serialises the form state into the current back-stack entry's
            // savedStateHandle and navigates to the confirmation screen.
            Button(
                onClick = {
                    navController.currentBackStackEntry?.savedStateHandle?.apply {
                        set("priority", selectedPriority.name)
                        set("condition", condition)
                        set("message", message)
                        set("includeLocation", includeLocation)
                        set("allowRelay", allowRelay)
                    }
                    navController.navigate(Screen.SosConfirmation.route)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmergencyCritical
                ),
                enabled = condition.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Broadcast SOS", fontWeight = FontWeight.Bold)
            }
        }
    }
}
