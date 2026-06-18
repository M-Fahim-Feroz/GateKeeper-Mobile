package com.gatekeeper.mobile.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gatekeeper.mobile.ui.screens.protecthub.ProtectHubScreen
import com.gatekeeper.mobile.ui.screens.dashboard.DashboardScreen
import com.gatekeeper.mobile.ui.screens.dns.DnsFilterScreen
import com.gatekeeper.mobile.ui.screens.firewall.FirewallScreen
import com.gatekeeper.mobile.ui.screens.settings.SettingsScreen
import com.gatekeeper.mobile.ui.screens.settings.SettingsLandingScreen
import com.gatekeeper.mobile.ui.screens.settings.SettingsProtectionScreen
import com.gatekeeper.mobile.ui.screens.settings.SettingsPrivacyScreen
import com.gatekeeper.mobile.ui.screens.settings.SettingsAdvancedScreen
import com.gatekeeper.mobile.ui.screens.settings.SettingsAboutScreen
import com.gatekeeper.mobile.ui.screens.settings.LimitationsScreen
import com.gatekeeper.mobile.ui.screens.traffic.TrafficScreen
import com.gatekeeper.mobile.ui.screens.threats.ThreatFeedScreen
import com.gatekeeper.mobile.ui.screens.permissionauditor.PermissionAuditorScreen

import com.gatekeeper.mobile.ui.screens.wifiscanner.WifiScannerScreen
import com.gatekeeper.mobile.ui.screens.certaudit.CertAuditScreen
import com.gatekeeper.mobile.ui.theme.*
import com.gatekeeper.mobile.vpn.GateKeeperVpnService

// Routes where the bottom nav bar should be visible
private val BOTTOM_NAV_ROUTES = Screen.bottomNavItems.map { it.route }.toSet()

@Composable
fun AppNavigation(
    startDestination: String = Screen.Dashboard.route,
    deepLinkRoute: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()

    androidx.compose.runtime.LaunchedEffect(deepLinkRoute) {
        if (deepLinkRoute != null) {
            navController.navigate(deepLinkRoute)
            onDeepLinkConsumed()
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Only show bottom nav on primary tab screens
    val showBottomBar = currentRoute in BOTTOM_NAV_ROUTES

    // VPN status for badge
    val isVpnActive by GateKeeperVpnService.isRunning.collectAsState()

    AmbientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
            // ── Only show bottom nav on main 5 tabs ──────────────────────────
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(150))
            ) {
                Column {
                    // Gradient separator line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        LocalGKColors.current.surface,
                                        LocalGKColors.current.border.copy(alpha = 0.3f),
                                        LocalGKColors.current.surface
                                    )
                                )
                            )
                    )
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.glassPanel()
                    ) {
                        Screen.bottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    // VPN status badge on Dashboard tab
                                    if (screen == Screen.Dashboard) {
                                        BadgedBox(
                                            badge = {
                                                if (isVpnActive) {
                                                    Badge(
                                                        containerColor = LocalGKColors.current.statusOnline,
                                                        modifier = Modifier.size(8.dp)
                                                    )
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) screen.iconSelected else screen.icon,
                                                contentDescription = screen.title,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) screen.iconSelected else screen.icon,
                                            contentDescription = screen.title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = LocalGKColors.current.primary,
                                    selectedTextColor = LocalGKColors.current.primary,
                                    unselectedIconColor = LocalGKColors.current.textTertiary,
                                    unselectedTextColor = LocalGKColors.current.textTertiary,
                                    indicatorColor = LocalGKColors.current.primary.copy(alpha = 0.10f)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController) }
            composable(Screen.ProtectHub.route) { ProtectHubScreen(navController = navController) }
            composable(Screen.Firewall.route) { FirewallScreen(navController = navController) }
            composable(Screen.DnsFilter.route) { DnsFilterScreen(navController = navController) }
            composable(Screen.Traffic.route) { TrafficScreen(navController = navController) }
            composable(Screen.Alerts.route) { com.gatekeeper.mobile.ui.screens.alerts.AlertsScreen(navController = navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
            composable("settings/protection") { SettingsProtectionScreen(navController = navController) }
            composable("settings/privacy") { SettingsPrivacyScreen(navController = navController) }
            composable("settings/advanced") { SettingsAdvancedScreen(navController = navController) }
            composable("settings/about") { SettingsAboutScreen(navController = navController) }
            composable("limitations") { LimitationsScreen(navController = navController) }
            composable(Screen.ThreatFeed.route) { ThreatFeedScreen(navController = navController) }
            composable(Screen.PermissionAuditor.route) { PermissionAuditorScreen(navController = navController) }

            composable(Screen.WifiScanner.route) { WifiScannerScreen(navController = navController) }
            composable(Screen.CertAudit.route) { CertAuditScreen(navController = navController) }
            composable("onboarding") {
                com.gatekeeper.mobile.ui.screens.onboarding.OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
} // end AmbientBackground
} // end AppNavigation
