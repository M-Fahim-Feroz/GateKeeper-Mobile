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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
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
    val blockedCount by GateKeeperVpnService.blockedCount.collectAsState()

    val appsProtected by viewModel.blockedAppsCount.collectAsState(initial = 0)
    val dnsFiltered by viewModel.dnsBlockedCount.collectAsState(initial = 0)
    val threatCount by viewModel.threatCount.collectAsState(initial = 0)
    val rogueCertsCount by viewModel.rogueCertsCount.collectAsState()
    val securityAlerts by viewModel.unresolvedAlerts.collectAsState(initial = emptyList())
    val allAlerts by viewModel.allAlerts.collectAsState(initial = emptyList())
    val securityScore by viewModel.securityScore.collectAsState(initial = 100)
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
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Hero Banner ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(
                    colors = listOf(
                        if (isVpnActive) PrimaryCyan.copy(alpha = 0.08f) else AccentRed.copy(alpha = 0.06f),
                        DarkBackground
                    )
                ))
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top bar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("GateKeeper", style = MaterialTheme.typography.displayMedium, color = TextPrimary)
                        Text("Mobile Security Suite", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, "Settings", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // VPN Hero Card
                VpnHeroCard(
                    isActive = isVpnActive,
                    onToggle = {
                        if (isVpnActive) {
                            context.startService(Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_STOP })
                        } else {
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent != null) vpnLauncher.launch(prepareIntent)
                            else context.startForegroundService(Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_START })
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

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

            // ── Live Security Alerts Feed ────────────────────────────────────
            if (securityAlerts.isNotEmpty() || rogueCertsCount > 0) {
                SectionHeader(title = "⚠️ Active Security Alerts (${securityAlerts.size + if (rogueCertsCount > 0) 1 else 0})")
                Spacer(Modifier.height(6.dp))

                if (rogueCertsCount > 0) {
                    ThreatAlertBanner(
                        icon = Icons.Filled.GppBad,
                        title = "Rogue SSL Certificate Detected",
                        description = "$rogueCertsCount user-installed CA certificate(s) found. MITM attack possible.",
                        severity = "CRITICAL",
                        onClick = { navController.navigate("cert_audit") },
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
                        "+${securityAlerts.size - 3} more alerts — go to Threat Intel",
                        style = MaterialTheme.typography.bodySmall,
                        color = AccentOrange,
                        modifier = Modifier.clickable { navController.navigate("threat_feed") }.padding(4.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(16.dp))
            }

            // ── Background Sensor Access Warning ────────────────────────────
            if (bgSensorCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentOrange.copy(alpha = 0.1f))
                        .clickable { navController.navigate("permission_auditor") }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Sensors, null, tint = AccentOrange, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("$bgSensorCount Background Sensor Access${if (bgSensorCount > 1) "es" else ""} Today", style = MaterialTheme.typography.titleSmall, color = AccentOrange, fontWeight = FontWeight.Bold)
                        Text("Apps accessed Camera/Mic/Location in background. Tap to review.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Security Tools Grid ─────────────────────────────────────────────
            SectionHeader(title = "Security Tools")
            Spacer(Modifier.height(8.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = Icons.Filled.Shield, title = "App Firewall", subtitle = "Block app access",
                    gradientColors = GradientSuccess, onClick = { navController.navigate("firewall") }, modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Filled.Dns, title = "DNS Filter", subtitle = "Block ads & trackers",
                    gradientColors = GradientPurple, onClick = { navController.navigate("dns") }, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = Icons.Filled.WifiTethering, title = "Wi-Fi Scanner", subtitle = "Analyze local network",
                    gradientColors = GradientTeal, onClick = { navController.navigate("wifi_scanner") }, modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Filled.VerifiedUser, title = "Hardware Audit", subtitle = "App permissions",
                    gradientColors = GradientOrange, onClick = { navController.navigate("permission_auditor") }, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = Icons.Filled.Security, title = "Cert Auditor", subtitle = "Verify CA store",
                    gradientColors = GradientDanger, onClick = { navController.navigate("cert_audit") }, modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    icon = Icons.Filled.Language, title = "Threat Intel", subtitle = "Live blocklists",
                    gradientColors = listOf(Color(0xFF6C63FF), Color(0xFF3B33C3)), onClick = { navController.navigate("threat_feed") }, modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Recent Activity ─────────────────────────────────────────────
            val recentTraffic by viewModel.recentTraffic.collectAsState(initial = emptyList())
            SectionHeader(title = "Recent Activity")
            Spacer(Modifier.height(8.dp))

            if (recentTraffic.isEmpty()) {
                if (!isVpnActive) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(DarkCard).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable VPN to see live traffic", color = TextTertiary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent != null) vpnLauncher.launch(prepareIntent)
                            else context.startForegroundService(Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_START })
                        }) { Text("Enable", color = PrimaryCyan) }
                    }
                } else {
                    Text("No network activity recorded yet", color = TextTertiary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                }
            } else {
                recentTraffic.forEach { log -> MiniTrafficRow(log) }
                TextButton(onClick = { navController.navigate("traffic") }, modifier = Modifier.fillMaxWidth()) {
                    Text("See all traffic →", color = PrimaryCyan, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Detection Engines ─────────────────────────────────────────────
            GKSectionHeader("Engine Status")
            GKListRow(icon = Icons.Filled.Shield, title = "VPN Tunnel",
                subtitle = if (isVpnActive) "Active — all blocking rules enforced" else "Disabled — no blocking active",
                trailing = { GKToggle(isVpnActive) { 
                    if (isVpnActive) context.startService(Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_STOP })
                    else {
                        val prepareIntent = VpnService.prepare(context)
                        if (prepareIntent != null) vpnLauncher.launch(prepareIntent)
                        else context.startForegroundService(Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_START })
                    }
                } },
                onClick = { navController.navigate("firewall") })

            GKListRow(icon = Icons.Filled.GppBad, title = "DNS Privacy Guard",
                subtitle = "Stops encrypted DNS bypasses",
                trailing = { GKToggle(isDnsLeakEnabled) { viewModel.setDnsLeakProtection(it) } },
                onClick = { navController.navigate("settings/protection") })

            GKListRow(icon = Icons.Filled.CellTower, title = "IMSI Detector",
                subtitle = "Fake cell tower warning",
                trailing = { GKToggle(isImsiEnabled) { viewModel.setImsiDetection(it) } },
                onClick = { navController.navigate("settings/privacy") })
                
            GKListRow(icon = Icons.Filled.Wifi, title = "Evil Twin Detect",
                subtitle = "Detect duplicate Wi-Fi APs",
                trailing = { GKToggle(isEvilTwinEnabled) { viewModel.setEvilTwinDetection(it) } },
                onClick = { navController.navigate("wifi_scanner") })
                
            GKListRow(icon = Icons.Filled.BugReport, title = "Bypass Detect",
                subtitle = "Detect hardcoded IP access",
                trailing = { GKToggle(isFirewallBypassEnabled) { viewModel.setFirewallBypassDetect(it) } },
                onClick = { navController.navigate("settings/protection") })
                
            GKListRow(icon = Icons.Filled.Mic, title = "BG Sensor Alert",
                subtitle = "Background camera/mic access",
                trailing = { GKToggle(isBgSensorEnabled) { viewModel.setBackgroundSensorAlerts(it) } },
                onClick = { navController.navigate("settings/privacy") })

            // ── Recent Alert History ──────────────────────────────────────────
            if (allAlerts.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                SectionHeader(title = "Alert History (Last ${allAlerts.take(5).size})")
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
                        Icon(Icons.Filled.DeleteSweep, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Clear All Resolved Alerts", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── Supporting Composables ──────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
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
        "CRITICAL" -> AccentRed
        "HIGH" -> AccentOrange
        "MEDIUM" -> PrimaryCyan
        else -> TextTertiary
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
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
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(severity, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            
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
        "CRITICAL" -> AccentRed
        "HIGH" -> AccentOrange
        "MEDIUM" -> PrimaryCyan
        else -> TextTertiary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkCard)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (alert.isResolved) TextTertiary else color))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(alert.title, style = MaterialTheme.typography.bodySmall, color = if (alert.isResolved) TextTertiary else TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(fmt.format(Date(alert.timestamp)), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        }
        if (alert.isResolved) {
            Text("Resolved", style = MaterialTheme.typography.labelSmall, color = AccentGreen)
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
    onToggle: () -> Unit
) {
    val bgBrush = if (isActive)
        Brush.linearGradient(listOf(StatusOnline.copy(alpha = 0.08f), PrimaryCyan.copy(alpha = 0.04f)))
    else
        Brush.linearGradient(listOf(DarkCard, DarkCardElevated))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(1.dp, if (isActive) StatusOnline.copy(alpha = 0.3f) else BorderDefault, RoundedCornerShape(20.dp))
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
                    .background(if (isActive) StatusOnline.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Filled.Shield else Icons.Filled.GppBad,
                    contentDescription = null,
                    tint = if (isActive) StatusOnline else AccentRed,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isActive) "System Protected" else "Protection Disabled", 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold, 
                    color = if (isActive) AccentGreen else AccentRed
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    if (isActive) "All network traffic is being filtered and monitored." else "Your device is currently unprotected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                FilledIconButton(
                    onClick = onToggle,
                    modifier = Modifier.height(44.dp).width(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isActive) StatusOnline.copy(alpha = 0.2f) else AccentRed.copy(alpha = 0.2f)
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isActive) Icons.Filled.Power else Icons.Filled.PowerOff,
                            contentDescription = "Toggle VPN",
                            tint = if (isActive) StatusOnline else AccentRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isActive) "Disconnect" else "Connect",
                            color = if (isActive) StatusOnline else AccentRed,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
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
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Brush.horizontalGradient(gradientColors.map { it.copy(alpha = 0.2f) }))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
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
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
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
        val iconTint = if (log.wasBlocked) AccentRed else AccentGreen
        Icon(
            if (log.wasBlocked) Icons.Filled.Block else Icons.Filled.CheckCircle,
            null, tint = iconTint, modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(log.appName ?: "System", style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
        Text(formatter.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}
