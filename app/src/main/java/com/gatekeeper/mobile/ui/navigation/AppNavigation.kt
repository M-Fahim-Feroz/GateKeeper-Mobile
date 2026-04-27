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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gatekeeper.mobile.ui.screens.aichat.AiChatScreen
import com.gatekeeper.mobile.ui.screens.dashboard.DashboardScreen
import com.gatekeeper.mobile.ui.screens.dns.DnsFilterScreen
import com.gatekeeper.mobile.ui.screens.firewall.FirewallScreen
import com.gatekeeper.mobile.ui.screens.settings.SettingsScreen
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
fun AppNavigation(startDestination: String = Screen.Dashboard.route) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Only show bottom nav on primary tab screens
    val showBottomBar = currentRoute in BOTTOM_NAV_ROUTES

    // VPN status for badge
    val isVpnActive by GateKeeperVpnService.isRunning.collectAsState()

    Scaffold(
        containerColor = DarkBackground,
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
                                        DarkSurface,
                                        GlassBorder.copy(alpha = 0.3f),
                                        DarkSurface
                                    )
                                )
                            )
                    )
                    NavigationBar(
                        containerColor = DarkSurface,
                        tonalElevation = 0.dp
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
                                                        containerColor = StatusOnline,
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
                                    selectedIconColor = PrimaryCyan,
                                    selectedTextColor = PrimaryCyan,
                                    unselectedIconColor = TextTertiary,
                                    unselectedTextColor = TextTertiary,
                                    indicatorColor = PrimaryCyan.copy(alpha = 0.10f)
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
            enterTransition = { slideInHorizontally(initialOffsetX = { 60 }, animationSpec = tween(200)) + fadeIn(tween(180)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -60 }, animationSpec = tween(200)) + fadeOut(tween(150)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -60 }, animationSpec = tween(200)) + fadeIn(tween(180)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { 60 }, animationSpec = tween(200)) + fadeOut(tween(150)) }
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController) }
            composable(Screen.Firewall.route) { FirewallScreen() }
            composable(Screen.DnsFilter.route) { DnsFilterScreen() }
            composable(Screen.Traffic.route) { TrafficScreen() }
            composable(Screen.AiChat.route) { AiChatScreen() }
            composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
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
}
