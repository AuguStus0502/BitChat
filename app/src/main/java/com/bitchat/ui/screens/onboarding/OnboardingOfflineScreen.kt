package com.bitchat.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bitchat.ui.navigation.Screen
import com.bitchat.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

/**
 * Data model representing a single page in the onboarding pager.
 *
 * @param title       Headline displayed prominently on the page.
 * @param description Explanatory body text shown below the title.
 * @param icon        Material icon illustrated at the top of the page.
 * @param buttonText  Label for the primary action button on this page
 *                    (e.g. "Next" for intermediate pages, "Get Started" for the final page).
 */
data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val buttonText: String
)

/**
 * First interactive screen after the splash, presenting a swipeable three-page onboarding pager.
 *
 * Part of the initial setup journey: **Splash → Onboarding (this screen) → Permissions → Home**.
 *
 * The three pages introduce the user to BitChat's core value propositions:
 * 1. **Offline Mesh** — device-to-device Bluetooth communication with no internet dependency.
 * 2. **Disaster Ready** — SOS relay through nearby peers for emergency scenarios.
 * 3. **Private by Design** — end-to-end encryption and local-only identity.
 *
 * After the final page the user proceeds to [Screen.Permissions]. A "Skip" button on
 * intermediate pages allows the user to jump directly to permissions.
 *
 * ### Accessibility Notes
 * - The primary action button is 56dp tall, exceeding the 48dp minimum touch-target size.
 * - Page indicators use size variation (12dp selected / 8dp unselected) in addition to
 *   opacity differences so colour-blind users can distinguish the active page.
 * - Large 120dp icons and high-contrast text ensure readability in bright outdoor lighting,
 *   which is important for disaster/outdoor use-cases.
 *
 * @param navController Standard Jetpack Navigation controller. On completion of the pager
 *        navigates to [Screen.Permissions], clearing the onboarding route from the back stack.
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingOfflineScreen(navController: NavController) {
    // ── Page Definitions ──────────────────────────────────────────────
    // NOTE: Placeholder content for onboarding pages will be refined in
    // later phases. The structure and messaging are representative of the
    // final experience.
    val pages = listOf(
        OnboardingPage(
            title = "Offline Mesh",
            description = "BitChat communicates directly between devices using Bluetooth. No internet, phone number, or email required.",
            icon = Icons.Default.Bluetooth,
            buttonText = "Next"
        ),
        OnboardingPage(
            title = "Disaster Ready",
            description = "Broadcast emergency SOS messages that relay through nearby devices. Help reaches further when every device becomes a relay.",
            icon = Icons.Default.Warning,
            buttonText = "Next"
        ),
        OnboardingPage(
            title = "Private by Design",
            description = "Your identity is local. Messages are end-to-end encrypted. No central server stores your data.",
            icon = Icons.Default.Lock,
            buttonText = "Get Started"
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // ── Swipeable Pager ────────────────────────────────────────────
        // Each page is a full-screen column with a large icon, title,
        // and description. The user swipes horizontally or taps "Next".
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            val currentPage = pages[page]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = currentPage.icon,
                    contentDescription = null,
                    modifier = Modifier.size(120.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = currentPage.title,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = currentPage.description,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // ── Page Indicators ────────────────────────────────────────────
        // Dot indicators: selected dot is larger and fully opaque;
        // unselected dots are smaller and semi-transparent.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (isSelected) 12.dp else 8.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Primary Action Button ──────────────────────────────────────
        // Advances to the next page, or navigates to Permissions on the
        // final page. Button height is 56dp for a large touch target.
        Button(
            onClick = {
                if (pagerState.currentPage < pages.size - 1) {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    navController.navigate(Screen.Permissions.route) {
                        popUpTo(Screen.OnboardingOffline.route) { inclusive = true }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = pages[pagerState.currentPage].buttonText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // ── Skip Button ────────────────────────────────────────────────
        // Hidden on the final page since "Get Started" serves as the
        // forward action. Allows impatient users to skip the tutorial.
        if (pagerState.currentPage < pages.size - 1) {
            TextButton(
                onClick = {
                    navController.navigate(Screen.Permissions.route) {
                        popUpTo(Screen.OnboardingOffline.route) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
