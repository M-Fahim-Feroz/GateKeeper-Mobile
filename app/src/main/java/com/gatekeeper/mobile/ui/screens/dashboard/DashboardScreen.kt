package com.gatekeeper.mobile.ui.screens.dashboard

import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.navigation.safeNavigate
import com.gatekeeper.mobile.data.db.entity.SecurityAlert
import com.gatekeeper.mobile.ui.components.*
import com.gatekeeper.mobile.ui.theme.*
import com.gatekeeper.mobile.vpn.GateKeeperVpnService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isVpnActive by GateKeeperVpnService.isRunning.collectAsState()
    val isConnecting by GateKeeperVpnService.isConnecting.collectAsState()
    val blockedCount by GateKeeperVpnService.blockedCount.collectAsState()

    val appsProtected by viewModel.blockedAppsCount.collectAsState(initial = 0)
    val dnsFiltered by viewModel.dnsBlockedCount.collectAsState(initial = 0)
    val threatCount by viewModel.threatCount.collectAsState(initial = 0)
    val rogueCertsCount by viewModel.rogueCertsCount.collectAsState()
    val securityAlerts by viewModel.unresolvedAlerts.collectAsState(initial = emptyList())
    val allAlerts by viewModel.allAlerts.collectAsState(initial = emptyList())
    val bgSensorCount by viewModel.recentBackgroundSensorAccess.collectAsState(initial = 0)

    // Feature toggle states
    val isDnsLeakEnabled by viewModel.isDnsLeakEnabled.collectAsState()
    val isDnsExfilEnabled by viewModel.isDnsExfilEnabled.collectAsState()
    val isImsiEnabled by viewModel.isImsiDetectionEnabled.collectAsState()
    val isFirewallBypassEnabled by viewModel.isFirewallBypassEnabled.collectAsState()
    val isBgSensorEnabled by viewModel.isBgSensorAlertsEnabled.collectAsState()
    val isEvilTwinEnabled by viewModel.isEvilTwinEnabled.collectAsState()

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val intent = Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_START }
        context.startForegroundService(intent)
    }

    LaunchedEffect(Unit) {
        viewModel.rescanCerts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .verticalScroll(rememberScrollState())
    ) {
        // â”€â”€ Hero Banner â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(
                    colors = listOf(
                        if (isVpnActive) LocalGKColors.current.primary.copy(alpha = 0.08f) else LocalGKColors.current.accentRed.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                ))
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top bar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(
                                id = if (isSystemInDarkTheme()) com.gatekeeper.mobile.R.drawable.gk_logo_dark else com.gatekeeper.mobile.R.drawable.gk_logo_light
                            ),
                            contentDescription = "GateKeeper Logo",
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("GateKeeper", style = MaterialTheme.typography.displayMedium, color = LocalGKColors.current.textPrimary)
                            Text("Mobile Security Suite", style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textSecondary)
                        }
                    }
                    IconButton(onClick = { navController.safeNavigate("settings") }) {
                        Icon(Icons.Filled.Settings, "Settings", tint = LocalGKColors.current.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // VPN Hero Card
                VpnHeroCard(
                    isActive = isVpnActive,
                    isConnecting = isConnecting,
                    onToggle = {
                        if (isConnecting) return@VpnHeroCard
                        if (isVpnActive) {
                            context.startService(Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_STOP })
                        } else {
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent != null) vpnLauncher.launch(prepareIntent)
                            else context.startForegroundService(Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_START })
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Blocked", "$blockedCount", GradientDanger, modifier = Modifier.weight(1f))
                    StatCard("Apps", "$appsProtected", GradientSuccess, modifier = Modifier.weight(1f))
                    StatCard("DNS", "$dnsFiltered", GradientPurple, modifier = Modifier.weight(1f))
                    StatCard("Threats", "$threatCount", GradientOrange, modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

            // â”€â”€ Live Security Alerts Feed â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (securityAlerts.isNotEmpty() || rogueCertsCount > 0) {
                SectionHeader(title = "âš ï¸ Active Security Alerts (${securityAlerts.size + if (rogueCertsCount > 0) 1 else 0})")
                Spacer(Modifier.height(6.dp))

                if (rogueCertsCount > 0) {
                    ThreatAlertBanner(
                        icon = Icons.Filled.GppBad,
                        title = "Rogue SSL Certificate Detected",
                        description = "$rogueCertsCount user-installed CA certificate(s) found. MITM attack possible.",
                        severity = "CRITICAL",
                        onClick = { navController.safeNavigate("cert_audit") },
                        onResolve = null
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Show top 3 alerts, grouped by type
                val displayed = securityAlerts.take(3)
                displayed.forEach { alert ->
                    ThreatAlertBanner(
                        icon = alertIcon(alert.type),
                        title = alert.title,
                        description = alert.description,
                        severity = alert.severity,
                        onClick = null,
                        onResolve = { scope.launch { viewModel.resolveAlert(alert.id) } }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                if (securityAlerts.size > 3) {
                    Text(
                        "+${securityAlerts.size - 3} more alerts â€” go to Threat Intel",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalGKColors.current.accentOrange,
                        modifier = Modifier.clickable { navController.safeNavigate("threat_feed") }.padding(4.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(16.dp))
            }

            // â”€â”€ Background Sensor Access Warning â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (bgSensorCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalGKColors.current.accentOrange.copy(alpha = 0.1f))
                        .clickable { navController.safeNavigate("permission_auditor") }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Sensors, null, tint = LocalGKColors.current.accentOrange, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("$bgSensorCount Background Sensor Access${if (bgSensorCount > 1) "es" else ""} Today", style = MaterialTheme.typography.titleSmall, color = LocalGKColors.current.accentOrange, fontWeight = FontWeight.Bold)
                        Text("Apps accessed Camera/Mic/Location in background. Tap to review.", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = LocalGKColors.current.textTertiary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            // â”€â”€ Setup Checklist â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            val showChecklist = !isVpnActive || appsProtected == 0 || threatCount == 0
            if (showChecklist) {
                DashSectionHeader("Getting Started")
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(LocalGKColors.current.card)
                        .padding(16.dp)
                ) {
                    ChecklistItem(
                        title = "Start VPN Protection",
                        isDone = isVpnActive,
                        onClick = {
                            if (!isVpnActive) {
                                val prepareIntent = VpnService.prepare(context)
                                if (prepareIntent != null) vpnLauncher.launch(prepareIntent)
                                else context.startForegroundService(Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_START })
                            }
                        }
                    )
                    HorizontalDivider(color = LocalGKColors.current.border, modifier = Modifier.padding(vertical = 8.dp))
                    ChecklistItem(
                        title = "Secure Apps with Firewall",
                        isDone = appsProtected > 0,
                        onClick = { if (appsProtected == 0) navController.safeNavigate("firewall") }
                    )
                    HorizontalDivider(color = LocalGKColors.current.border, modifier = Modifier.padding(vertical = 8.dp))
                    ChecklistItem(
                        title = "Enable DNS Threat Feeds",
                        isDone = threatCount > 0,
                        onClick = { if (threatCount == 0) navController.safeNavigate("threat_feed") }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // â”€â”€ Security Tools Grid â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            DashSectionHeader(title = "Security Tools")
            Spacer(Modifier.height(8.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = Icons.Filled.Shield, title = "App Firewall", subtitle = "Block app access",
                    gradientColors = GradientSuccess, onClick = { navController.safeNavigate("firewall") }, modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Filled.Dns, title = "DNS Filter", subtitle = "Block ads & trackers",
                    gradientColors = GradientPurple, onClick = { navController.safeNavigate("dns") }, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = Icons.Filled.WifiTethering, title = "Wi-Fi Scanner", subtitle = "Analyze local network",
                    gradientColors = GradientTeal, onClick = { navController.safeNavigate("wifi_scanner") }, modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Filled.Security, title = "Cert Auditor", subtitle = "Verify CA store",
                    gradientColors = GradientDanger, onClick = { navController.safeNavigate("cert_audit") }, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = Icons.Filled.VerifiedUser, title = "Device Audit", subtitle = "Permissions & HW",
                    gradientColors = GradientOrange, onClick = { navController.safeNavigate("permission_auditor") }, modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Filled.Language, title = "Threat Intel", subtitle = "Live blocklists",
                    gradientColors = listOf(Color(0xFF6C63FF), Color(0xFF3B33C3)), onClick = { navController.safeNavigate("threat_feed") }, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            // Grid completes the 6 items

            Spacer(Modifier.height(16.dp))

            // Limitations Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = { navController.safeNavigate("limitations") }) {
                    Icon(Icons.Filled.Info, null, tint = LocalGKColors.current.textTertiary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Using demo data? See Technical Notes â†’", color = LocalGKColors.current.textTertiary, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(24.dp))

            // â”€â”€ Recent Activity â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            val recentTraffic by viewModel.recentTraffic.collectAsState(initial = emptyList())
            DashSectionHeader(title = "Recent Activity")
            Spacer(Modifier.height(8.dp))

            if (recentTraffic.isEmpty()) {
                if (!isVpnActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(LocalGKColors.current.card).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable VPN to see live traffic", color = LocalGKColors.current.textTertiary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent != null) vpnLauncher.launch(prepareIntent)
                            else context.startForegroundService(Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_START })
                        }) { Text("Enable", color = LocalGKColors.current.primary) }
                    }
                } else {
                    Text("No network activity recorded yet", color = LocalGKColors.current.textTertiary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            } else {
                recentTraffic.forEach { log -> MiniTrafficRow(log) }
                TextButton(onClick = { navController.safeNavigate("traffic") }, modifier = Modifier.fillMaxWidth()) {
                    Text("See all traffic â†’", color = LocalGKColors.current.primary, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(24.dp))

            // â”€â”€ Detection Engines â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            GKSectionHeader("Engine Status")
            GKListRow(icon = Icons.Filled.Shield, title = "VPN Tunnel",
                subtitle = if (isVpnActive) "Active â€” all blocking rules enforced" else "Disabled â€” no blocking active",
                trailing = { GKToggle(isVpnActive) { 
                    if (isVpnActive) context.startService(Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_STOP })
                    else {
                        val prepareIntent = VpnService.prepare(context)
                        if (prepareIntent != null) vpnLauncher.launch(prepareIntent)
                        else context.startForegroundService(Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_START })
                    }
                } },
                onClick = { navController.safeNavigate("firewall") })

            GKListRow(icon = Icons.Filled.GppBad, title = "DNS Privacy Guard",
                subtitle = "Stops encrypted DNS bypasses",
                trailing = { GKToggle(isDnsLeakEnabled) { viewModel.setDnsLeakProtection(it) } },
                onClick = { navController.safeNavigate("settings/protection") })

            GKListRow(icon = Icons.Filled.CellTower, title = "IMSI Detector",
                subtitle = "Fake cell tower warning",
                badge = "OS LIMITED",
                trailing = { GKToggle(isImsiEnabled) { viewModel.setImsiDetection(it) } },
                onClick = { navController.safeNavigate("settings/privacy") })
                
            GKListRow(icon = Icons.Filled.Wifi, title = "Evil Twin Detect",
                subtitle = "Detect duplicate Wi-Fi APs",
                badge = "OS LIMITED",
                trailing = { GKToggle(isEvilTwinEnabled) { viewModel.setEvilTwinDetection(it) } },
                onClick = { navController.safeNavigate("wifi_scanner") })
                
            GKListRow(icon = Icons.Filled.BugReport, title = "Bypass Detect",
                subtitle = "Detect hardcoded IP access",
                trailing = { GKToggle(isFirewallBypassEnabled) { viewModel.setFirewallBypassDetect(it) } },
                onClick = { navController.safeNavigate("settings/protection") })
                
            GKListRow(icon = Icons.Filled.Mic, title = "BG Sensor Alert",
                subtitle = "Background camera/mic access",
                trailing = { GKToggle(isBgSensorEnabled) { viewModel.setBackgroundSensorAlerts(it) } },
                onClick = { navController.safeNavigate("settings/privacy") })

            // â”€â”€ Recent Alert History â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (allAlerts.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                DashSectionHeader(title = "Alert History (Last ${allAlerts.take(5).size})")
                Spacer(Modifier.height(6.dp))
                val fmt = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
                allAlerts.take(5).forEach { alert ->
                    AlertHistoryRow(alert, fmt)
                    Spacer(Modifier.height(6.dp))
                }
                if (securityAlerts.isNotEmpty()) {
                    TextButton(
                        onClick = { scope.launch { viewModel.clearAllAlerts() } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.DeleteSweep, null, tint = LocalGKColors.current.textTertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear All Resolved Alerts", color = LocalGKColors.current.textTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// â”€â”€â”€ Supporting Composables â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
fun DashSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = LocalGKColors.current.textPrimary
    )
}



@Composable
fun ThreatAlertBanner(
    icon: ImageVector,
    title: String,
    description: String,
    severity: String,
    onClick: (() -> Unit)?,
    onResolve: (() -> Unit)?
) {
    val color = when (severity) {
        "CRITICAL" -> LocalGKColors.current.accentRed
        "HIGH" -> LocalGKColors.current.accentOrange
        "MEDIUM" -> LocalGKColors.current.primary
        else -> LocalGKColors.current.textTertiary
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassPanel(shape = RoundedCornerShape(12.dp))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(severity, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
            
            if (onResolve != null) {
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onResolve, modifier = Modifier.height(28.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)) {
                        Text("Mark as Resolved", color = color, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun AlertHistoryRow(alert: SecurityAlert, fmt: SimpleDateFormat) {
    val color = when (alert.severity) {
        "CRITICAL" -> LocalGKColors.current.accentRed
        "HIGH" -> LocalGKColors.current.accentOrange
        "MEDIUM" -> LocalGKColors.current.primary
        else -> LocalGKColors.current.textTertiary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LocalGKColors.current.card)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (alert.isResolved) LocalGKColors.current.textTertiary else color))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(alert.title, style = MaterialTheme.typography.bodySmall, color = if (alert.isResolved) LocalGKColors.current.textTertiary else LocalGKColors.current.textPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(fmt.format(Date(alert.timestamp)), style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary)
        }
        if (alert.isResolved) {
            Text("Resolved", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.accentGreen)
        } else {
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text(alert.severity, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun alertIcon(type: String): ImageVector = when (type) {
    "IMSI_CATCHER" -> Icons.Filled.CellTower
    "DNS_EXFILTRATION" -> Icons.Filled.Sensors
    "DNS_LEAK" -> Icons.Filled.GppBad
    "FIREWALL_BYPASS" -> Icons.Filled.BugReport
    "EVIL_TWIN" -> Icons.Filled.WifiOff
    "ROGUE_CA" -> Icons.Filled.GppBad
    "EXFILTRATION" -> Icons.Filled.UploadFile
    else -> Icons.Filled.Warning
}

@Composable
fun VpnHeroCard(
    isActive: Boolean, 
    isConnecting: Boolean,
    onToggle: () -> Unit
) {
    val bgBrush = if (isActive)
        Brush.linearGradient(listOf(LocalGKColors.current.statusOnline.copy(alpha = 0.08f), LocalGKColors.current.primary.copy(alpha = 0.04f)))
    else
        Brush.linearGradient(listOf(LocalGKColors.current.card, LocalGKColors.current.cardElevated))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(shape = RoundedCornerShape(20.dp))
            .cyberGlowBorder(LocalGKColors.current.borderFocus, 20.dp, isActive)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prominent Shield Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (isActive) LocalGKColors.current.statusOnline.copy(alpha = 0.15f) else LocalGKColors.current.accentRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Filled.Shield else Icons.Filled.GppBad,
                    contentDescription = null,
                    tint = if (isActive) LocalGKColors.current.statusOnline else LocalGKColors.current.accentRed,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isActive) "System Protected" else "Protection Disabled", 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold, 
                    color = if (isActive) LocalGKColors.current.accentGreen else LocalGKColors.current.accentRed
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    if (isActive) "All network traffic is being filtered and monitored." else "Your device is currently unprotected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalGKColors.current.textSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                FilledIconButton(
                    onClick = onToggle,
                    modifier = Modifier.height(44.dp).width(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isConnecting,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isActive) LocalGKColors.current.statusOnline.copy(alpha = 0.2f) else LocalGKColors.current.accentRed.copy(alpha = 0.2f),
                        disabledContainerColor = LocalGKColors.current.textTertiary.copy(alpha = 0.2f)
                    )
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = LocalGKColors.current.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isActive) Icons.Filled.Power else Icons.Filled.PowerOff,
                                contentDescription = "Toggle VPN",
                                tint = if (isActive) LocalGKColors.current.statusOnline else LocalGKColors.current.accentRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isActive) "Disconnect" else "Connect",
                                color = if (isActive) LocalGKColors.current.statusOnline else LocalGKColors.current.accentRed,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    icon: ImageVector, title: String, subtitle: String,
    gradientColors: List<Color>, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().glassCard(shape = RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(gradientColors.map { it.copy(alpha = 0.18f) })),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = gradientColors.first(), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = LocalGKColors.current.textPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textTertiary)
        }
    }
}

@Composable
fun MiniTrafficRow(log: com.gatekeeper.mobile.data.db.entity.ConnectionLog) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconTint = if (log.wasBlocked) LocalGKColors.current.accentRed else LocalGKColors.current.accentGreen
        Icon(
            if (log.wasBlocked) Icons.Filled.Block else Icons.Filled.CheckCircle,
            null, tint = iconTint, modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(log.appName ?: "System", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
        Text(formatter.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary)
    }
}

// Removed DeviceAuditBannerCard as per user request to move it to grid

@Composable
fun ChecklistItem(title: String, isDone: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape)
                .background(if (isDone) LocalGKColors.current.accentGreen.copy(alpha = 0.2f) else LocalGKColors.current.surface),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Filled.Check, null, tint = LocalGKColors.current.accentGreen, modifier = Modifier.size(16.dp))
            } else {
                Box(Modifier.size(8.dp).clip(CircleShape).background(LocalGKColors.current.textTertiary))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDone) LocalGKColors.current.textSecondary else LocalGKColors.current.textPrimary,
            fontWeight = if (isDone) FontWeight.Normal else FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (!isDone) {
            Icon(Icons.Filled.ChevronRight, null, tint = LocalGKColors.current.textTertiary, modifier = Modifier.size(16.dp))
        }
    }
}

