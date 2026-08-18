package com.bitchat.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.bitchat.core.models.SosPriority
import com.bitchat.core.models.MessageStatus
import com.bitchat.ui.theme.*

/**
 * Emergency & Status Indicator Components
 *
 * Design Philosophy:
 * These components communicate life-critical information: emergency severity levels,
 * message delivery status, and mesh signal strength. The humanitarian design principle
 * demands that these indicators are:
 *
 * 1. **Immediately scannable** — users must assess status in under 2 seconds
 * 2. **Never ambiguous** — color + icon + text provide triple-redundant communication
 * 3. **Calm under pressure** — colors are saturated but not jarring; the UI should
 *    feel like a trusted aide, not an alarm panel
 * 4. **Accessible** — all indicators work for users with color vision deficiencies
 *    through the use of distinct icons and text labels alongside color
 *
 * Emergency severity follows a universally understood traffic-light metaphor:
 * - Red (CRITICAL) = immediate danger, life-threatening
 * - Orange (HELP_NEEDED) = urgent assistance required
 * - Green (STABLE) = situation is under control or resolved
 *
 * This metaphor transcends language barriers, which is essential for a
 * humanitarian tool used across diverse communities.
 */

/**
 * A compact badge displaying the priority level of an SOS emergency message.
 *
 * The badge uses a solid, high-saturation background color to ensure maximum
 * visibility against any surface. The white text and icon on the colored background
 * maintain strong contrast (exceeding 7:1 ratio) for readability in stressful
 * viewing conditions.
 *
 * @param priority The [SosPriority] level determining the badge color, label, and icon.
 * @param modifier Optional [Modifier] for additional layout customization.
 */
@Composable
fun PriorityBadge(priority: SosPriority, modifier: Modifier = Modifier) {
    // Map each priority level to its visual representation.
    // The Triple contains: (color, label text, icon) for consistent destructuring.
    val (color, label, icon) = when (priority) {
        SosPriority.CRITICAL -> Triple(EmergencyCritical, "CRITICAL", Icons.Default.Error)
        SosPriority.HELP_NEEDED -> Triple(EmergencyHelpNeeded, "HELP", Icons.Default.Warning)
        SosPriority.STABLE -> Triple(EmergencyStable, "STABLE", Icons.Default.Info)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

/**
 * A small icon indicator showing the delivery status of a sent message.
 *
 * In a mesh network, message delivery is not instantaneous — messages may be
 * queued, relayed through multiple peers, or fail entirely. This indicator
 * gives senders immediate feedback on their message's journey through the network.
 *
 * Each status is represented by a distinct icon and color:
 * - [MessageStatus.QUEUED] — gray clock icon, message waiting to be sent
 * - [MessageStatus.SENDING] — orange send icon, actively transmitting
 * - [MessageStatus.RELAYING] — orange hotel/hub icon, being forwarded by a peer
 * - [MessageStatus.DELIVERED] — green double-check icon, confirmed received
 * - [MessageStatus.FAILED] — red error icon, delivery unsuccessful
 * - [MessageStatus.EXPIRED] — gray timer-off icon, message TTL has elapsed
 *
 * @param status The current [MessageStatus] of the message to display.
 * @param modifier Optional [Modifier] for additional layout customization.
 */
@Composable
fun MessageStatusIndicator(status: MessageStatus, modifier: Modifier = Modifier) {
    val (color, icon) = when (status) {
        MessageStatus.QUEUED -> Pair(StatusQueued, Icons.Default.Schedule)
        MessageStatus.SENDING -> Pair(StatusSending, Icons.AutoMirrored.Filled.Send)
        MessageStatus.RELAYING -> Pair(StatusSending, Icons.Default.Hotel)
        MessageStatus.DELIVERED -> Pair(StatusDelivered, Icons.Default.DoneAll)
        MessageStatus.FAILED -> Pair(StatusFailed, Icons.Default.ErrorOutline)
        MessageStatus.EXPIRED -> Pair(StatusQueued, Icons.Default.TimerOff)
    }

    Icon(
        imageVector = icon,
        contentDescription = status.name,
        modifier = modifier.size(16.dp),
        tint = color
    )
}

/**
 * An inline indicator displaying Bluetooth mesh signal strength with a
 * colored dot and text label.
 *
 * This component is designed for compact layouts (toolbars, status bars)
 * where a full status chip would be too wide. The dot-and-label pattern
 * is universally recognizable and works well at small sizes.
 *
 * Color semantics:
 * - Green ([SignalStrong]) — robust mesh connection, optimal conditions
 * - Orange ([SignalImproving]) — connection recovering, may be intermittent
 * - Red ([SignalWeak]) — poor connection, messages may need relay hops
 * - Gray ([SignalLost]) — no active mesh connection detected
 *
 * @param strength The current [SignalStrength] level to display.
 * @param modifier Optional [Modifier] for additional layout customization.
 */
@Composable
fun SignalWarmthIndicator(
    strength: SignalStrength,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (strength) {
        SignalStrength.STRONG -> Pair(SignalStrong, "Strong")
        SignalStrength.IMPROVING -> Pair(SignalImproving, "Improving")
        SignalStrength.WEAK -> Pair(SignalWeak, "Weak")
        SignalStrength.LOST -> Pair(SignalLost, "Lost")
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color
        )
    }
}

/**
 * Represents the four levels of Bluetooth mesh signal quality in BitChat.
 *
 * The name "SignalStrength" is intentionally neutral and descriptive — avoiding
 * technical jargon like "RSSI" or "dBm" that would be meaningless to most
 * emergency volunteers and community members using the app.
 */
enum class SignalStrength {
    STRONG, IMPROVING, WEAK, LOST
}
