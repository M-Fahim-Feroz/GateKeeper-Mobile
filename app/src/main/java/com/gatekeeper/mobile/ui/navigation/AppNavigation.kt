package com.gatekeeper.mobile.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.gatekeeper.mobile.ui.theme.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
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
                                Icon(
                                    imageVector = if (selected) screen.iconSelected else screen.icon,
                                    contentDescription = screen.title,
                                    modifier = Modifier.size(22.dp)
                                )
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
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
        }
    }
}
