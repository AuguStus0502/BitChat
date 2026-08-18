package com.bitchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bitchat.ui.theme.*

/**
 * Status Chip Components
 *
 * Design Philosophy:
 * Status chips provide at-a-glance information about the device's connectivity
 * and resource state. In emergency scenarios, users need to quickly assess whether
 * their communication channels are functional. These chips use a humanitarian
 * design language: color indicates state without causing alarm, and the rounded,
 * compact form factor keeps the interface feeling approachable rather than technical.
 *
 * Each chip combines an icon, a descriptive label, and a value — providing redundant
 * communication channels (visual, textual, and color-coded) to ensure that users
 * with color vision deficiencies can still interpret the status accurately.
 *
 * Accessibility Notes:
 * - Chips use color + icon + text (triple redundancy) for status communication
 * - The 12dp corner radius creates a soft, non-threatening visual shape
 * - Text contrast is maintained by using the semantic color at full opacity for
 *   the value and at 70% opacity for the label (both remain above 4.5:1 contrast)
 * - The chip background uses the semantic color at 12% opacity — subtle enough to
 *   not overwhelm, but visible enough to tint the surface meaningfully
 */

/**
 * A generic, reusable status chip that displays an icon, label, and value
 * in a compact, rounded container.
 *
 * This is the foundational building block for all status indicators in BitChat.
 * The chip adapts its color scheme based on the [color] parameter, applying it
 * to the background (at 12% opacity), icon, label text, and value text.
 *
 * @param label Short descriptor text displayed above the value (e.g., "Bluetooth", "Battery").
 * @param value The current status value displayed prominently (e.g., "3 peers", "85%").
 * @param icon Material icon displayed to the left of the text content.
 * @param color Semantic color representing the current state; applied to all chip elements.
 * @param modifier Optional [Modifier] for additional layout customization.
 */
@Composable
fun StatusChip(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        contentColor = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = color
                )
            }
        }
    }
}

/**
 * Displays the current Bluetooth mesh connectivity status.
 *
 * Shows whether Bluetooth is active and, if connected, how many mesh peers
 * are within range. This is critical information in emergency scenarios where
 * the mesh network is the primary communication channel.
 *
 * Color semantics:
 * - Green ([SignalStrong]) — Bluetooth is on and connected to peers
 * - Gray ([SignalLost]) — Bluetooth is off or unavailable
 *
 * @param connected Whether Bluetooth is currently enabled and connected to the mesh.
 * @param peerCount The number of nearby mesh peers detected via Bluetooth.
 */
@Composable
fun BluetoothStatusChip(connected: Boolean, peerCount: Int) {
    StatusChip(
        label = "Bluetooth",
        value = if (connected) "$peerCount peers" else "Off",
        icon = Icons.Default.Bluetooth,
        color = if (connected) SignalStrong else SignalLost
    )
}

/**
 * Displays the device's internet network connectivity status.
 *
 * While BitChat primarily operates over Bluetooth mesh, network connectivity
 * enables features like GPS-assisted positioning and map data downloads.
 *
 * Color semantics:
 * - Green ([SignalStrong]) — device has an active internet connection
 * - Red ([SignalWeak]) — device is offline (note: not necessarily critical,
 *   since mesh communication still works without internet)
 *
 * @param online Whether the device currently has an active internet connection.
 */
@Composable
fun NetworkStatusChip(online: Boolean) {
    StatusChip(
        label = "Network",
        value = if (online) "Online" else "Offline",
        icon = Icons.Default.Wifi,
        color = if (online) SignalStrong else SignalWeak
    )
}

/**
 * Displays the device's current battery level as a percentage.
 *
 * Battery awareness is essential in disaster scenarios where charging may be
 * unavailable for extended periods. The chip uses a three-tier color system
 * to communicate urgency without causing panic.
 *
 * Color semantics:
 * - Green ([SignalStrong]) — battery above 50%; healthy reserve
 * - Amber ([TertiaryAmber]) — battery between 20-50%; conserve power
 * - Red ([SignalWeak]) — battery below 20%; critical, seek charging
 *
 * @param level Battery percentage as an integer from 0 to 100.
 */
@Composable
fun BatteryChip(level: Int) {
    val color = when {
        level > 50 -> SignalStrong
        level > 20 -> TertiaryAmber
        else -> SignalWeak
    }
    StatusChip(
        label = "Battery",
        value = "$level%",
        icon = Icons.Default.BatteryStd,
        color = color
    )
}
