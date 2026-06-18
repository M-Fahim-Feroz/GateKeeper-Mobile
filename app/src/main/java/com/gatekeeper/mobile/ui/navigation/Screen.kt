package com.gatekeeper.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations for bottom nav and screen routing.
 * bottomNavItems = exactly 4 tabs: Home | Protect | Activity | Settings
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val iconSelected: ImageVector
) {
    // ── Bottom nav tabs (4 only) ──────────────────────────────────────────────
    data object Dashboard : Screen(
        route = "dashboard",
        title = "Home",
        icon = Icons.Outlined.Home,
        iconSelected = Icons.Filled.Home
    )
    data object ProtectHub : Screen(
        route = "protect_hub",
        title = "Protect",
        icon = Icons.Outlined.VerifiedUser,
        iconSelected = Icons.Filled.VerifiedUser
    )
    data object Alerts : Screen(
        route = "alerts",
        title = "Alerts",
        icon = Icons.Outlined.Notifications,
        iconSelected = Icons.Filled.Notifications
    )
    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        icon = Icons.Outlined.Settings,
        iconSelected = Icons.Filled.Settings
    )

    // ── Module screens (navigated from ProtectHub / Dashboard) ────────────────
    data object Firewall : Screen(
        route = "firewall",
        title = "App Gate",
        icon = Icons.Outlined.Policy,
        iconSelected = Icons.Filled.Policy
    )
    data object DnsFilter : Screen(
        route = "dns",
        title = "Web Gate",
        icon = Icons.Outlined.Wifi,
        iconSelected = Icons.Filled.Wifi
    )
    data object ThreatFeed : Screen(
        route = "threat_feed",
        title = "Threat Intel",
        icon = Icons.Outlined.Security,
        iconSelected = Icons.Filled.Security
    )
    data object WifiScanner : Screen(
        route = "wifi_scanner",
        title = "Wi-Fi Guard",
        icon = Icons.Outlined.WifiTethering,
        iconSelected = Icons.Filled.WifiTethering
    )
    data object CertAudit : Screen(
        route = "cert_audit",
        title = "Trust Check",
        icon = Icons.Outlined.VerifiedUser,
        iconSelected = Icons.Filled.VerifiedUser
    )
    data object PermissionAuditor : Screen(
        route = "permission_auditor",
        title = "Privacy Scan",
        icon = Icons.Outlined.Security,
        iconSelected = Icons.Filled.Security
    )

    data object Traffic : Screen(
        route = "traffic",
        title = "NetWatch",
        icon = Icons.Outlined.BarChart,
        iconSelected = Icons.Filled.BarChart
    )

    companion object {
        /** The 5 bottom navigation tabs */
        val bottomNavItems = listOf(Dashboard, Firewall, DnsFilter, Traffic, PermissionAuditor)
    }
}
