package com.gatekeeper.mobile.ui.screens.dashboard

import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.gatekeeper.mobile.ui.components.*
import com.gatekeeper.mobile.ui.theme.*
import com.gatekeeper.mobile.vpn.GateKeeperVpnService

@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isVpnActive by GateKeeperVpnService.isRunning.collectAsState()
    val blockedCount by GateKeeperVpnService.blockedCount.collectAsState()

    val appsProtected by viewModel.blockedAppsCount.collectAsState(initial = 0)
    val dnsFiltered by viewModel.dnsBlockedCount.collectAsState(initial = 0)
    val threatCount by viewModel.threatCount.collectAsState(initial = 0)
    val connectionCount by viewModel.connectionCount.collectAsState(initial = 0)

    val vpnLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val intent = Intent(context, GateKeeperVpnService::class.java).apply {
            action = GateKeeperVpnService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // â”€â”€ Hero Banner â”€â”€
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (isVpnActive) PrimaryCyan.copy(alpha = 0.08f) else AccentRed.copy(alpha = 0.06f),
                            DarkBackground
                        )
                    )
                )
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // â”€â”€ Top bar â”€â”€
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "GateKeeper",
                            style = MaterialTheme.typography.displayMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Mobile Security Suite",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, "Settings", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // â”€â”€ VPN Hero Card â”€â”€
                VpnHeroCard(
                    isActive = isVpnActive,
                    onToggle = {
                        if (isVpnActive) {
                            context.startService(
                                Intent(context, GateKeeperVpnService::class.java).apply {
                                    action = GateKeeperVpnService.ACTION_STOP
                                }
                            )
                        } else {
                            val prepareIntent = VpnService.prepare(context)
                            if (prepareIntent != null) vpnLauncher.launch(prepareIntent)
                            else context.startForegroundService(
                                Intent(context, GateKeeperVpnService::class.java).apply {
                                    action = GateKeeperVpnService.ACTION_START
                                }
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // â”€â”€ Stats Row (4-column) â”€â”€
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard("Blocked", "$blockedCount", GradientDanger, modifier = Modifier.weight(1f))
                    StatCard("Apps", "$appsProtected", GradientSuccess, modifier = Modifier.weight(1f))
                    StatCard("DNS", "$dnsFiltered", GradientPurple, modifier = Modifier.weight(1f))
                    StatCard("Threats", "$threatCount", GradientOrange, modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // â”€â”€ Security Modules â”€â”€
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            SectionHeader(title = "Security Modules")

            Spacer(modifier = Modifier.height(4.dp))

            val modules = listOf(
                ModuleItem(Icons.Filled.Shield, "App Firewall", "VPN split-tunnel per-app control", GradientPrimary, "firewall"),
                ModuleItem(Icons.Filled.Dns, "DNS Filter", "Block domains at the DNS level", GradientPurple, "dns"),
                ModuleItem(Icons.Filled.NetworkCheck, "Traffic Monitor", "Real-time connection tracking", GradientSuccess, "traffic"),
                ModuleItem(Icons.Filled.SmartToy, "AI Assistant", "Natural-language security commands", GradientOrange, "ai_chat"),
                ModuleItem(Icons.Filled.Security, "Threat Intel", "Import known malicious IPs & domains", GradientDanger, "threat_feed"),
                ModuleItem(Icons.Filled.VerifiedUser, "Permission Audit", "Identify privacy-risk apps", listOf(AccentTeal, PrimaryBlue), "permission_auditor"),
                ModuleItem(Icons.Filled.WifiTethering, "Wi-Fi Security", "Analyze network vulnerabilities", GradientTeal, "wifi_scanner"),
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private data class ModuleItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>,
    val route: String
)

@Composable
fun VpnHeroCard(isActive: Boolean, onToggle: () -> Unit) {
    val borderColor by animateColorAsState(
        targetValue = if (isActive) StatusOnline.copy(alpha = 0.5f) else GlassBorder,
        animationSpec = tween(600),
        label = "vpn_border"
    )
    val bgBrush = if (isActive)
        Brush.linearGradient(listOf(StatusOnline.copy(alpha = 0.06f), PrimaryCyan.copy(alpha = 0.04f)))
    else
        Brush.linearGradient(listOf(DarkCard, DarkCardElevated))

    // Pulsing ring animation
    val infiniteTransition = rememberInfiniteTransition(label = "vpn_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgBrush)
            .then(
                Modifier.background(
                    GlassBackground,
                    shape
                )
            )
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Animated shield icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    if (isActive) StatusOnline.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Filled.ShieldMoon else Icons.Outlined.Shield,
                contentDescription = null,
                tint = if (isActive) StatusOnline else AccentRed,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "VPN Protection",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            StatusBadge(
                isActive = isActive,
                activeText = "Protected",
                inactiveText = "Unprotected"
            )
        }

        // Toggle button
        FilledIconButton(
            onClick = onToggle,
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isActive) StatusOnline.copy(alpha = 0.2f) else AccentRed.copy(alpha = 0.2f)
            )
        ) {
            Icon(
                imageVector = if (isActive) Icons.Filled.Power else Icons.Filled.PowerOff,
                contentDescription = "Toggle VPN",
                tint = if (isActive) StatusOnline else AccentRed,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.horizontalGradient(gradientColors.map { it.copy(alpha = 0.2f) })
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(gradientColors.map { it.copy(alpha = 0.18f) })),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = gradientColors.first(),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = TextTertiary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
