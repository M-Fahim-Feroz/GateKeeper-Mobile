package com.gatekeeper.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations for the bottom nav and screen routing.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val iconSelected: ImageVector
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "Dashboard",
        icon = Icons.Outlined.Dashboard,
        iconSelected = Icons.Filled.Dashboard
    )
    data object Firewall : Screen(
        route = "firewall",
        title = "Firewall",
        icon = Icons.Outlined.Shield,
        iconSelected = Icons.Filled.Shield
    )
    data object DnsFilter : Screen(
        route = "dns",
        title = "DNS",
        icon = Icons.Outlined.Dns,
        iconSelected = Icons.Filled.Dns
    )
    data object Traffic : Screen(
        route = "traffic",
        title = "Traffic",
        icon = Icons.Outlined.NetworkCheck,
        iconSelected = Icons.Filled.NetworkCheck
    )
    data object AiChat : Screen(
        route = "ai_chat",
        title = "AI",
        icon = Icons.Outlined.SmartToy,
        iconSelected = Icons.Filled.SmartToy
    )
    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        icon = Icons.Outlined.Settings,
        iconSelected = Icons.Filled.Settings
    )
    
    // Non-bottom-nav screens
    data object ThreatFeed : Screen(
        route = "threat_feed",
        title = "Threat Feed",
        icon = Icons.Outlined.Security,
        iconSelected = Icons.Filled.Security
    )
    data object PermissionAuditor : Screen(
        route = "permission_auditor",
        title = "Device Audit",
        icon = Icons.Outlined.VerifiedUser,
        iconSelected = Icons.Filled.VerifiedUser
    )
    data object WifiScanner : Screen(
        route = "wifi_scanner",
        title = "Wi-Fi Scanner",
        icon = Icons.Outlined.WifiTethering,
        iconSelected = Icons.Filled.WifiTethering
    )
    data object CertAudit : Screen(
        route = "cert_audit",
        title = "Cert Auditor",
        icon = Icons.Outlined.Security,
        iconSelected = Icons.Filled.Security
    )

    companion object {
        // Bottom nav: Dashboard, Firewall, DNS, Traffic, Settings
        // AI Chat is kept as a route but removed from the nav bar
        val bottomNavItems = listOf(Dashboard, Firewall, DnsFilter, Traffic, Settings)
    }
}
