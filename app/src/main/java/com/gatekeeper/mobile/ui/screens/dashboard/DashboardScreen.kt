package com.gatekeeper.mobile.ui.screens.dashboard

import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.navigation.Screen
import com.gatekeeper.mobile.ui.navigation.safeNavigate
import com.gatekeeper.mobile.ui.components.GKInfoButton
import com.gatekeeper.mobile.ui.components.GKInfoDialog
import com.gatekeeper.mobile.ui.components.HtmlToggleSwitch
import com.gatekeeper.mobile.ui.theme.*
import com.gatekeeper.mobile.vpn.GateKeeperVpnService

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val isVpnActive by GateKeeperVpnService.isRunning.collectAsState()
    val isConnecting by GateKeeperVpnService.isConnecting.collectAsState()

    val appsProtected by viewModel.blockedAppsCount.collectAsState(initial = 0)
    val dnsFiltered by viewModel.dnsBlockedCount.collectAsState(initial = 0)
    val threatCount by viewModel.threatCount.collectAsState(initial = 0)
    val rogueCertsCount by viewModel.rogueCertsCount.collectAsState()
    val sensorAccesses by viewModel.recentBackgroundSensorAccess.collectAsState(initial = 0)

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val intent = Intent(context, GateKeeperVpnService::class.java).apply { action = GateKeeperVpnService.ACTION_START }
        context.startForegroundService(intent)
    }

    LaunchedEffect(Unit) {
        viewModel.rescanCerts()
    }

    data class SelectedStatDialogData(val title: String, val items: List<Pair<String, String>>)
    var selectedStatInfo by remember { mutableStateOf<SelectedStatDialogData?>(null) }
    var showEnginesInfoDialog by remember { mutableStateOf(false) }
    var showVpnInfoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalGKColors.current.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp) // Bottom nav padding
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalGKColors.current.primary.copy(alpha = 0.1f))
                        .border(1.dp, LocalGKColors.current.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = LocalGKColors.current.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("GateKeeper", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LocalGKColors.current.primary)
                    Text("Mobile Security Suite", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textSecondary)
                }
            }
            IconButton(onClick = { navController.safeNavigate(Screen.Settings.route) }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = LocalGKColors.current.primary)
            }
        }

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
            },
            onInfoClick = { showVpnInfoDialog = true }
        )

        // Stats Grid
        val colorError = LocalGKColors.current.accentRed
        val colorTertiary = LocalGKColors.current.accentGreen
        val colorSecondary = Color(0xFFCDBDFF) // Purple from Stitch
        val colorAmber = LocalGKColors.current.accentOrange

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardStatCard(
                    title = "Active Threat Filters",
                    value = threatCount.toString(),
                    color = colorError,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        selectedStatInfo = SelectedStatDialogData(
                            title = "Active Threat Filters",
                            items = listOf(
                                "Threat Intelligence Feeds" to "Sourced from external threat databases (e.g., OTX, Abuse.ch) synced to your device.",
                                "Custom Threat Rules" to "Sourced from user-defined threat signatures.",
                                "Total Active Rules" to "$threatCount rules currently loaded in the protection engine."
                            )
                        )
                    }
                )
                DashboardStatCard(
                    title = "Apps Blocked",
                    value = appsProtected.toString(),
                    color = colorTertiary,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        selectedStatInfo = SelectedStatDialogData(
                            title = "Apps Blocked",
                            items = listOf(
                                "User Apps" to "Sourced from your personal Firewall application rules.",
                                "System Apps" to "Sourced from system-level network restrictions.",
                                "Total Blocked Apps" to "$appsProtected apps currently restricted from network access."
                            )
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardStatCard(
                    title = "Custom Blocklist",
                    value = dnsFiltered.toString(),
                    color = colorSecondary,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        selectedStatInfo = SelectedStatDialogData(
                            title = "Custom Blocklist",
                            items = listOf(
                                "Domain Filters" to "Sourced from the Web Gate DNS filtering engine.",
                                "IP Filters" to "Sourced from custom network layer blocks.",
                                "Total Custom Rules" to "$dnsFiltered domains/IPs currently configured to be blocked."
                            )
                        )
                    }
                )
                DashboardStatCard(
                    title = "User Certificates",
                    value = rogueCertsCount.toString(),
                    color = colorAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        selectedStatInfo = SelectedStatDialogData(
                            title = "User Certificates",
                            items = listOf(
                                "Legitimate User Certs" to "Sourced from Android System Credential Store. Certificates installed by you or your organization.",
                                "Suspicious Certs" to "Sourced from Trust Check analysis. Certificates that could potentially be used for MitM attacks.",
                                "Total User Certs" to "$rogueCertsCount root certificates detected on your device."
                            )
                        )
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Protection Engines Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Protection Engines", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = LocalGKColors.current.textPrimary)
                GKInfoButton(color = LocalGKColors.current.primary) { showEnginesInfoDialog = true }
            }

            // Protection Engines Grid
            val modules = listOf(
                DashboardModuleItem("App Gate", "$appsProtected RESTRICTED", Icons.Filled.Apps, colorTertiary, Screen.Firewall.route),
                DashboardModuleItem("Web Gate", "$dnsFiltered DOMAINS", Icons.Filled.Public, colorSecondary, Screen.DnsFilter.route),
                DashboardModuleItem("Threat Intel", "$threatCount RULES", Icons.Filled.Radar, colorAmber, Screen.ThreatFeed.route),
                DashboardModuleItem("Wi-Fi Guard", "TAP TO SCAN", Icons.Filled.WifiTethering, LocalGKColors.current.primary, Screen.WifiScanner.route),
                DashboardModuleItem("Trust Check", "$rogueCertsCount INSTALLED", Icons.Filled.VerifiedUser, colorError, Screen.CertAudit.route),
                DashboardModuleItem("Privacy Logs", "$sensorAccesses RECENT", Icons.Filled.Policy, colorAmber, Screen.PermissionAuditor.route)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(0.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.heightIn(max = 600.dp) // prevents infinite height in scroll
            ) {
                items(modules) { module ->
                    ProtectionEngineCard(
                        module = module,
                        onClick = { navController.safeNavigate(module.route) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    selectedStatInfo?.let { statData ->
        InfoDialog(
            title = statData.title,
            items = statData.items,
            onDismiss = { selectedStatInfo = null }
        )
    }

    if (showVpnInfoDialog) {
        GKInfoDialog(
            title = "Protection VPN",
            body = "GateKeeper uses a local VPN to inspect and filter your phone's traffic — nothing leaves your device.\n\nWhen active, it routes all connections through a secure channel where GateKeeper can:\n• Block dangerous websites\n• Stop ads and tracker connections\n• Enforce per-app internet rules\n\nYour data is never sent to any external server.",
            accentColor = LocalGKColors.current.primary,
            onDismiss = { showVpnInfoDialog = false }
        )
    }
    if (showEnginesInfoDialog) {
        GKInfoDialog(
            title = "Protection Engines",
            body = "Each module protects a different part of your digital life:\n\n• App Gate — controls which apps can use the internet\n• Web Gate — blocks dangerous websites and ads\n• Threat Intel — blocks known malware and hacker servers\n• Wi-Fi Guard — detects fake and dangerous wireless networks\n• Trust Check — scans for suspicious security certificates\n• Privacy Logs — shows when apps access your mic, camera or sensors",
            accentColor = LocalGKColors.current.primary,
            onDismiss = { showEnginesInfoDialog = false }
        )
    }
}

data class DashboardModuleItem(
    val name: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

@Composable
fun ProtectionEngineCard(module: DashboardModuleItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(144.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        // Background Glow
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(96.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(module.color.copy(alpha = 0.1f), Color.Transparent),
                        radius = 150f
                    )
                )
        )
        
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(module.icon, contentDescription = null, tint = module.color, modifier = Modifier.size(32.dp))
            Column {
                Text(module.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = LocalGKColors.current.textPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(module.subtitle, style = MaterialTheme.typography.labelSmall, color = module.color.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun DashboardStatCard(title: String, value: String, color: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Box(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        // Blur circle
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 16.dp, y = (-16).dp)
                .size(64.dp)
                .background(color.copy(alpha = 0.1f), CircleShape)
        )
        
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.displaySmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VpnHeroCard(
    isActive: Boolean, 
    isConnecting: Boolean,
    onToggle: () -> Unit,
    onInfoClick: (() -> Unit)? = null
) {
    val statusColor = if (isActive) LocalGKColors.current.accentGreen else LocalGKColors.current.textSecondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .height(260.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
    ) {
        IconButton(
            onClick = { onInfoClick?.invoke() },
            modifier = Modifier.align(Alignment.TopEnd).padding(24.dp)
                .size(32.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.03f))
        ) {
            Icon(Icons.Filled.Info, null, tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(20.dp))
        }

        // Pulse rings
        if (isActive) {
            val infiniteTransition = rememberInfiniteTransition()
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(animation = tween(3000), repeatMode = RepeatMode.Restart)
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(animation = tween(3000), repeatMode = RepeatMode.Restart)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-30).dp)
                    .size(220.dp)
                    .scale(scale)
                    .border(1.dp, statusColor.copy(alpha = alpha), CircleShape)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 32.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Massive Power Button Toggle
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (isActive) statusColor.copy(alpha = 0.15f) else Color.Black)
                    .border(2.dp, if (isActive) statusColor else statusColor.copy(alpha = 0.3f), CircleShape)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                // Background glow inside button
                if (isActive) {
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.radialGradient(listOf(statusColor.copy(alpha = 0.4f), Color.Transparent))
                    ))
                }
                Icon(Icons.Filled.PowerSettingsNew, null, tint = statusColor, modifier = Modifier.size(40.dp))
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when {
                        isConnecting -> "Connecting..."
                        isActive -> "System Protected"
                        else -> "Protection Paused"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = LocalGKColors.current.textSecondary
                    )
                } else {
                    Text(
                        if (isActive) "Secure tunnel active" else "Tap power button to enable",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LocalGKColors.current.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun InfoDialog(
    title: String,
    items: List<Pair<String, String>>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textPrimary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                items.forEach { (name, description) ->
                    Text(name, fontWeight = FontWeight.SemiBold, color = LocalGKColors.current.primary, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textSecondary)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = LocalGKColors.current.primary)
            }
        },
        containerColor = LocalGKColors.current.background,
        titleContentColor = LocalGKColors.current.textPrimary,
        textContentColor = LocalGKColors.current.textSecondary
    )
}
