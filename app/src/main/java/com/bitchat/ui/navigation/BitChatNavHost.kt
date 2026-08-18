package com.bitchat.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bitchat.ui.screens.splash.SplashScreen
import com.bitchat.ui.screens.onboarding.OnboardingOfflineScreen
import com.bitchat.ui.screens.onboarding.OnboardingDisasterScreen
import com.bitchat.ui.screens.onboarding.OnboardingPrivacyScreen
import com.bitchat.ui.screens.onboarding.PermissionsScreen
import com.bitchat.ui.screens.home.HomeScreen
import com.bitchat.ui.screens.emergency.SosComposerScreen
import com.bitchat.ui.screens.emergency.SosConfirmationScreen
import com.bitchat.ui.screens.emergency.ActiveBeaconScreen
import com.bitchat.ui.screens.emergency.NearbySosFeedScreen
import com.bitchat.ui.screens.emergency.SosDetailScreen
import com.bitchat.ui.screens.private.PeerDiscoveryScreen
import com.bitchat.ui.screens.private.ContactTokenScreen
import com.bitchat.ui.screens.private.HandshakeVerificationScreen
import com.bitchat.ui.screens.private.EphemeralChatScreen
import com.bitchat.ui.screens.panic.PanicWipeConfirmScreen
import com.bitchat.ui.screens.panic.PanicWipeCompleteScreen
import com.bitchat.ui.screens.settings.SettingsScreen
import com.bitchat.ui.screens.diagnostics.DiagnosticsScreen

/**
 * Root Composable that wires every [Screen] to its destination screen.
 *
 * This host is the single source of truth for the app's navigation graph.
 * The [NavHostController] manages the back stack, handles system-back presses,
 * and provides animated transitions between destinations.
 *
 * ## Design decisions
 *
 * * **Parameter passing** — Routes that need runtime data (beacon IDs, peer IDs)
 *   declare `{placeholder}` segments in the route template. The `navArgument`
 *   declarations let Compose Navigation parse them from the URL, and the
 *   corresponding `backStackEntry.arguments?.getString(...)` call extracts the
 *   value on the screen side.
 * * **Back-stack management** — The controller is created once via
 *   [rememberNavController] and survives recomposition. All child screens receive
 *   the same instance so they can push, pop, or navigate conditionally without
 *   duplicating state.
 * * **Start destination** — Defaults to [Screen.Splash] but can be overridden
 *   for deep-link testing or restoring from a saved state.
 *
 * @param navController the navigation controller; callers may supply a custom
 *   instance for testing or preview purposes.
 * @param startDestination the first route loaded into the back stack.
 */
@Composable
fun BitChatNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        // ── First-launch / onboarding flow ─────────────────────────────────

        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        composable(Screen.OnboardingOffline.route) {
            OnboardingOfflineScreen(navController = navController)
        }
        composable(Screen.OnboardingDisaster.route) {
            OnboardingDisasterScreen(navController = navController)
        }
        composable(Screen.OnboardingPrivacy.route) {
            OnboardingPrivacyScreen(navController = navController)
        }
        composable(Screen.Permissions.route) {
            PermissionsScreen(navController = navController)
        }

        // ── Core hub ───────────────────────────────────────────────────────

        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        // ── Emergency / SOS flow ───────────────────────────────────────────

        composable(Screen.SosComposer.route) {
            SosComposerScreen(navController = navController)
        }
        composable(Screen.SosConfirmation.route) {
            SosConfirmationScreen(navController = navController)
        }

        // ActiveBeacon requires a beaconId path parameter to display the
        // correct beacon state. The argument is extracted from the back-stack
        // entry so the screen is fully driven by the navigation graph.
        composable(
            route = Screen.ActiveBeacon.route,
            arguments = listOf(navArgument("beaconId") { type = NavType.StringType })
        ) { backStackEntry ->
            val beaconId = backStackEntry.arguments?.getString("beaconId") ?: ""
            ActiveBeaconScreen(beaconId = beaconId, navController = navController)
        }

        composable(Screen.NearbySosFeed.route) {
            NearbySosFeedScreen(navController = navController)
        }

        // SosDetail mirrors ActiveBeacon's parameterised pattern — the caller
        // uses Screen.SosDetail.createRoute(id) to push this destination.
        composable(
            route = Screen.SosDetail.route,
            arguments = listOf(navArgument("beaconId") { type = NavType.StringType })
        ) { backStackEntry ->
            val beaconId = backStackEntry.arguments?.getString("beaconId") ?: ""
            SosDetailScreen(beaconId = beaconId, navController = navController)
        }

        // ── Private-messaging flow ─────────────────────────────────────────

        composable(Screen.PeerDiscovery.route) {
            PeerDiscoveryScreen(navController = navController)
        }
        composable(Screen.ContactToken.route) {
            ContactTokenScreen(navController = navController)
        }

        // HandshakeVerification is navigated to from PeerDiscovery with an
        // opaque peer fingerprint that identifies the remote device.
        composable(
            route = Screen.HandshakeVerification.route,
            arguments = listOf(navArgument("peerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
            HandshakeVerificationScreen(peerId = peerId, navController = navController)
        }

        // EphemeralChat is the final destination in the private-messaging flow.
        // The chat session exists only in memory and is discarded when the
        // user navigates back or activates a panic wipe.
        composable(
            route = Screen.EphemeralChat.route,
            arguments = listOf(navArgument("peerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
            EphemeralChatScreen(peerId = peerId, navController = navController)
        }

        // ── Panic wipe flow ────────────────────────────────────────────────

        composable(Screen.PanicWipeConfirm.route) {
            PanicWipeConfirmScreen(navController = navController)
        }
        composable(Screen.PanicWipeComplete.route) {
            PanicWipeCompleteScreen(navController = navController)
        }

        // ── Utility screens ────────────────────────────────────────────────

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(Screen.Diagnostics.route) {
            DiagnosticsScreen(navController = navController)
        }
    }
}
