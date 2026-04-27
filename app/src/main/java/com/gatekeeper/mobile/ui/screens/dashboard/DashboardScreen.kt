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
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
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
    val connectionCount by viewModel.connectionCount.collectAsState(initial = 0)
    val rogueCertsCount by viewModel.rogueCertsCount.collectAsState()
    val securityAlerts by viewModel.unresolvedAlerts.collectAsState(initial = emptyList())
    val allAlerts by viewModel.allAlerts.collectAsState(initial = emptyList())
    val securityScore by viewModel.securityScore.collectAsState(initial = 100)
    val bgSensorCount by viewModel.recentBackgroundSensorAccess.collectAsState(initial = 0)

    // Feature toggle states for status grid
    val isDnsLeakEnabled by viewModel.isDnsLeakEnabled.collectAsState()
    val isDnsExfilEnabled by viewModel.isDnsExfilEnabled.collectAsState()
    val isScreenOffEnabled by viewModel.isScreenOffEnabled.collectAsState()
    val isImsiEnabled by viewModel.isImsiDetectionEnabled.collectAsState()
    val isFirewallBypassEnabled by viewModel.isFirewallBypassEnabled.collectAsState()
    val isBgSensorEnabled by viewModel.isBgSensorAlertsEnabled.collectAsState()
    val isEvilTwinEnabled by viewModel.isEvilTwinEnabled.collectAsState()

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val intent = Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_START }
        context.startForegroundService(intent)
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
                    securityScore = securityScore,
                    onToggle = {
                        if (isVpnActive) {
                            context.startService(Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_STOP })
                        } else {
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent != null) vpnLauncher.launch(prepareIntent)
                            else context.startForegroundService(Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_START })
                        }
                    },
                    rogueCertsCount = rogueCertsCount,
                    criticalAlertsCount = securityAlerts.count { it.severity == "CRITICAL" },
                    highAlertsCount = securityAlerts.count { it.severity == "HIGH" },
                    isDnsLeakEnabled = isDnsLeakEnabled,
                    isImsiEnabled = isImsiEnabled,
                    navController = navController
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

                Spacer(Modifier.height(4.dp))
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
                Spacer(Modifier.height(12.dp))
            }

            // ── Security Feature Status Grid ─────────────────────────────────
            SectionHeader(title = "Detection Engines")
            Spacer(Modifier.height(8.dp))

            val featureStatuses = listOf(
                FeatureStatus("VPN Tunnel", isVpnActive, Icons.Filled.Shield, if (isVpnActive) PrimaryCyan else AccentRed, "firewall"),
                FeatureStatus("DNS Leak Guard", isDnsLeakEnabled, Icons.Filled.GppBad, if (isDnsLeakEnabled) PrimaryCyan else TextTertiary, "dns"),
                FeatureStatus("DNS Exfil Detect", isDnsExfilEnabled, Icons.Filled.Sensors, if (isDnsExfilEnabled) AccentYellow else TextTertiary, "settings"),
                FeatureStatus("IMSI Detector", isImsiEnabled, Icons.Filled.CellTower, if (isImsiEnabled) AccentRed else TextTertiary, "settings"),
                FeatureStatus("Screen-Off Block", isScreenOffEnabled, Icons.Filled.NightlightRound, if (isScreenOffEnabled) AccentOrange else TextTertiary, "firewall"),
                FeatureStatus("Bypass Detect", isFirewallBypassEnabled, Icons.Filled.BugReport, if (isFirewallBypassEnabled) AccentRed else TextTertiary, "settings"),
                FeatureStatus("BG Sensor Alert", isBgSensorEnabled, Icons.Filled.Mic, if (isBgSensorEnabled) AccentOrange else TextTertiary, "permission_auditor"),
                FeatureStatus("Evil Twin Detect", isEvilTwinEnabled, Icons.Filled.Wifi, if (isEvilTwinEnabled) AccentYellow else TextTertiary, "wifi_scanner"),
            )

            // 2-column grid
            featureStatuses.chunked(2).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { feature ->
                        FeatureStatusCard(feature, modifier = Modifier.weight(1f)) {
                            navController.navigate(feature.route)
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))

            // ── Security Modules ─────────────────────────────────────────────
            SectionHeader(title = "Security Modules")
            Spacer(Modifier.height(4.dp))

            val modules = listOf(
                ModuleItem(Icons.Filled.Shield, "App Firewall", "Per-app VPN split-tunnel + screen-off blocking", GradientPrimary, "firewall"),
                ModuleItem(Icons.Filled.Dns, "DNS Filter", "Domain sinkhole + exfiltration detection", GradientPurple, "dns"),
                ModuleItem(Icons.Filled.NetworkCheck, "Traffic Monitor", "Real-time per-app connection tracking", GradientSuccess, "traffic"),
                ModuleItem(Icons.Filled.SmartToy, "AI Assistant", "Natural-language security analysis", GradientOrange, "ai_chat"),
                ModuleItem(Icons.Filled.Security, "Threat Intel", "Malicious IP/domain feed management", GradientDanger, "threat_feed"),
                ModuleItem(Icons.Filled.VerifiedUser, "Privacy Guard", "Hardware sensor access + permission audit", listOf(AccentTeal, PrimaryBlue), "permission_auditor"),
                ModuleItem(Icons.Filled.WifiTethering, "Wi-Fi Security", "Evil Twin & rogue AP analysis", GradientTeal, "wifi_scanner"),
                ModuleItem(Icons.Filled.GppBad, "SSL Auditor", "Rogue certificate authority detection", listOf(AccentOrange, AccentRed), "cert_audit"),
            )

            modules.forEach { module ->
                QuickActionCard(
                    icon = module.icon,
                    title = module.title,
                    subtitle = module.subtitle,
                    gradientColors = module.gradientColors,
                    onClick = { navController.navigate(module.route) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Recent Alert History ──────────────────────────────────────────
            if (allAlerts.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ─── Supporting Composables ──────────────────────────────────────────────────

internal data class ModuleItem(val icon: ImageVector, val title: String, val subtitle: String, val gradientColors: List<Color>, val route: String)
internal data class FeatureStatus(val label: String, val isActive: Boolean, val icon: ImageVector, val color: Color, val route: String)

@Composable
fun ThreatAlertBanner(
    icon: ImageVector,
    title: String,
    description: String,
    severity: String,
    onClick: (() -> Unit)?,
    onResolve: (() -> Unit)?
) {
    val (bgColor, textColor) = when (severity) {
        "CRITICAL" -> AccentRed.copy(alpha = 0.12f) to AccentRed
        "HIGH" -> AccentOrange.copy(alpha = 0.10f) to AccentOrange
        "MEDIUM" -> AccentYellow.copy(alpha = 0.08f) to AccentYellow
        else -> PrimaryCyan.copy(alpha = 0.08f) to PrimaryCyan
    }
    
    var expanded by remember { mutableStateOf(false) }
    
    val mitigationText = when {
        title.contains("IMSI") || title.contains("Downgrade") -> "A fake cell tower is forcing your phone to 2G encryption. Do not send SMS or make standard calls right now. Use WhatsApp/Signal instead."
        title.contains("Rogue") || title.contains("Certificate") -> "A rogue certificate can read your HTTPS traffic (like passwords). Go to Cert Auditor to review and remove it."
        title.contains("Bypass") -> "An app bypassed the DNS filter using a hardcoded IP address. Check the Firewall to block this app if it's untrusted."
        title.contains("DNS Leak") -> "An app bypassed GateKeeper using an encrypted DoH server (e.g. 1.1.1.1). Traffic may not be filtered."
        title.contains("Evil Twin") -> "A fake Wi-Fi network with the same name as yours is trying to steal your connection. Disconnect from Wi-Fi immediately."
        else -> "Review your security logs and ensure the VPN is active."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { if (onClick != null) onClick() else expanded = !expanded }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(textColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = textColor, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = textColor, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(textColor.copy(alpha = 0.15f)).padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(severity, style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Bold)
                    }
                }
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = if (expanded) Int.MAX_VALUE else 2)
            }
            if (onResolve != null) {
                IconButton(onClick = onResolve, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.CheckCircle, "Resolve", tint = textColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            } else if (onClick == null) {
                // If it's expandable but has no click action, show an expand icon
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
            }
        }

        // Expanded mitigation advice
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Divider(color = GlassBorder)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Lightbulb, null, tint = AccentYellow, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        mitigationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureStatusCard(feature: FeatureStatus, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(feature.icon, null, tint = feature.color, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (feature.isActive) "ON" else "OFF",
                style = MaterialTheme.typography.labelSmall,
                color = if (feature.isActive) feature.color else TextTertiary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(feature.label, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun AlertHistoryRow(alert: SecurityAlert, fmt: SimpleDateFormat) {
    val color = when (alert.severity) {
        "CRITICAL" -> AccentRed
        "HIGH" -> AccentOrange
        "MEDIUM" -> AccentYellow
        else -> PrimaryCyan
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
    securityScore: Int, 
    onToggle: () -> Unit,
    rogueCertsCount: Int = 0,
    criticalAlertsCount: Int = 0,
    highAlertsCount: Int = 0,
    isDnsLeakEnabled: Boolean = true,
    isImsiEnabled: Boolean = true,
    navController: NavController? = null
) {
    var showBreakdown by remember { mutableStateOf(false) }
    
    val bgBrush = if (isActive)
        Brush.linearGradient(listOf(StatusOnline.copy(alpha = 0.06f), PrimaryCyan.copy(alpha = 0.04f)))
    else
        Brush.linearGradient(listOf(DarkCard, DarkCardElevated))

    val scoreColor = when {
        securityScore >= 80 -> AccentGreen
        securityScore >= 50 -> AccentOrange
        else -> AccentRed
    }

    val animatedScore by animateFloatAsState(
        targetValue = securityScore / 100f,
        animationSpec = tween(800),
        label = "score_anim"
    )

    val scoreLabel = when {
        securityScore >= 80 -> "Secure"
        securityScore >= 70 -> "Moderate"
        securityScore >= 50 -> "At Risk"
        else -> "Critical"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(1.dp, if (isActive) StatusOnline.copy(alpha = 0.3f) else BorderDefault, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ring Chart
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable { showBreakdown = !showBreakdown },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(100.dp)) {
                    // Track
                    drawArc(
                        color = BorderDefault,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    // Progress
                    drawArc(
                        color = scoreColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedScore,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = securityScore.toString(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                        color = scoreColor
                    )
                    Text(
                        text = scoreLabel,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("System Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                StatusBadge(isActive = isActive, activeText = "VPN Protected", inactiveText = "VPN Disabled")
                Spacer(modifier = Modifier.height(16.dp))
                FilledIconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = if (isActive) StatusOnline.copy(alpha = 0.2f) else AccentRed.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Filled.Power else Icons.Filled.PowerOff,
                        contentDescription = "Toggle VPN",
                        tint = if (isActive) StatusOnline else AccentRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Breakdown
        AnimatedVisibility(visible = showBreakdown) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Divider(color = BorderDefault)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Score Breakdown", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                if (!isActive) ScoreRow("VPN disabled", -35) { onToggle() }
                if (rogueCertsCount > 0) ScoreRow("Rogue certificate found", -20) { navController?.navigate("cert_audit") }
                if (criticalAlertsCount > 0) ScoreRow("Critical alerts ($criticalAlertsCount)", -20) { }
                if (highAlertsCount > 0) ScoreRow("High alerts ($highAlertsCount)", -8) { }
                if (!isDnsLeakEnabled) ScoreRow("DNS Leak Protection off", -5) { navController?.navigate("settings/protection") }
                if (!isImsiEnabled) ScoreRow("IMSI Detection off", -5) { navController?.navigate("settings/privacy") }
                
                if (isActive && rogueCertsCount == 0 && criticalAlertsCount == 0 && highAlertsCount == 0 && isDnsLeakEnabled && isImsiEnabled) {
                    Text("Your device is fully secured. No deductions.", style = MaterialTheme.typography.bodySmall, color = AccentGreen)
                }
            }
        }
    }
}

@Composable
fun ScoreRow(reason: String, deduction: Int, fix: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Warning, null, tint = AccentOrange, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(reason, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f))
        Text("$deduction pts", style = MaterialTheme.typography.labelSmall, color = AccentRed)
        Spacer(Modifier.width(12.dp))
        TextButton(onClick = fix, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
            Text("Fix \u2192", style = MaterialTheme.typography.labelSmall, color = PrimaryCyan)
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(gradientColors.map { it.copy(alpha = 0.18f) })),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = gradientColors.first(), modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(1.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextTertiary.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }
}
