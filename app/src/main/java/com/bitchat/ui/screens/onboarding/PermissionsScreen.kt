package com.bitchat.ui.screens.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bitchat.ui.navigation.Screen

/**
 * Runtime-permissions gate that sits between onboarding and the home screen.
 *
 * Part of the initial setup journey: **Splash → Onboarding → Permissions (this screen) → Home**.
 *
 * Requests the Bluetooth and location permissions required for BLE scanning,
 * advertising, and peer-to-peer communication. On Android 12+ (API 31) the
 * newer granular Bluetooth permissions are requested; on older versions the
 * legacy Bluetooth + location permissions are used instead.
 *
 * If all required permissions are already granted (e.g. on a re-launch),
 * this screen automatically navigates forward without user interaction.
 *
 * ### Permission Breakdown
 * | Permission               | Android Version | Purpose                              |
 * |--------------------------|-----------------|--------------------------------------|
 * | BLUETOOTH_SCAN           | 12+ (API 31)    | Scan for nearby BLE peripherals       |
 * | BLUETOOTH_ADVERTISE      | 12+ (API 31)    | Advertise as a BitChat peer           |
 * | BLUETOOTH_CONNECT        | 12+ (API 31)    | Connect to discovered peers           |
 * | ACCESS_FINE_LOCATION     | All             | Required by Android for BLE scanning  |
 * | POST_NOTIFICATIONS       | 13+ (API 33)    | Foreground-service connection alerts  |
 * | BLUETOOTH (legacy)       | < 12            | Classic Bluetooth access              |
 * | BLUETOOTH_ADMIN (legacy) | < 12            | Enable/disable Bluetooth              |
 *
 * ### Accessibility Notes
 * - The "Grant Permissions" button is 56dp tall, exceeding the 48dp minimum
 *   touch-target size for easy activation.
 * - Each [PermissionItem] card clearly labels the permission with both an
 *   icon and descriptive text, supporting screen readers (TalkBack/TTS).
 * - High-contrast text and large font sizes ensure readability in outdoor
 *   conditions typical for BitChat's disaster-use scenarios.
 *
 * @param navController Standard Jetpack Navigation controller. On grant or
 *        skip, navigates to [Screen.Home] and removes this route from the
 *        back stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(navController: NavController) {
    val context = LocalContext.current
    var permissionsGranted by remember { mutableStateOf(false) }

    // ── Permission Definitions ────────────────────────────────────────
    // API-level-aware permission set: Android 12+ uses the new granular
    // Bluetooth permissions; older versions use the legacy set.
    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    // ── Permission Launcher ───────────────────────────────────────────
    // Launches the system permission dialog for all required permissions
    // at once. On result, checks if every permission was granted.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        if (permissionsGranted) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Permissions.route) { inclusive = true }
            }
        }
    }

    // ── Auto-Skip Check ───────────────────────────────────────────────
    // If permissions are already granted (e.g. returning after a crash
    // or re-install with saved state), skip straight to Home.
    LaunchedEffect(Unit) {
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Permissions.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Permissions Required",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "BitChat needs Bluetooth and Location permissions to discover and communicate with nearby peers. Your data stays on your device.",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Permission Item Cards ─────────────────────────────────────
        // Visual breakdown of each permission and why it is needed,
        // supporting user trust and transparency.
        PermissionItem(
            icon = Icons.Default.Bluetooth,
            title = "Bluetooth",
            description = "Discover and connect to nearby peers"
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionItem(
            icon = Icons.Default.LocationOn,
            title = "Location",
            description = "Required for Bluetooth scanning on Android"
        )
        // Notifications permission is only available on Android 13+ (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Spacer(modifier = Modifier.height(12.dp))
            PermissionItem(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                description = "Service alerts for active connections"
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ── Grant Button ──────────────────────────────────────────────
        // 56dp height provides a generous touch target for all users.
        Button(
            onClick = { launcher.launch(requiredPermissions) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = "Grant Permissions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // ── Skip Fallback ─────────────────────────────────────────────
        // Allows the user to proceed without granting permissions.
        // Functionality will be limited but the app remains navigable.
        TextButton(
            onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Permissions.route) { inclusive = true }
                }
            }
        ) {
            Text("Skip for now")
        }
    }
}

/**
 * Reusable card that visually describes a single runtime permission.
 *
 * Displays a leading icon, a bold title, and a concise description of
 * why the permission is needed. Used on [PermissionsScreen] to build
 * trust and transparency before the system dialog appears.
 *
 * ### Accessibility Notes
 * - Cards use sufficient padding (16dp) for easy touch interaction.
 * - The icon tint matches the primary colour for consistent visual
 *   hierarchy. Descriptive text supports TalkBack TTS output.
 *
 * @param icon        Material icon representing the permission type.
 * @param title       Short label (e.g. "Bluetooth").
 * @param description One-line explanation of why the permission is needed.
 */
@Composable
private fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
