package com.gatekeeper.mobile.ui.screens.protecthub

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.components.GKListRow
import com.gatekeeper.mobile.ui.components.GKModuleHeader
import com.gatekeeper.mobile.ui.components.GKPrimaryButton
import com.gatekeeper.mobile.ui.components.SectionHeader
import com.gatekeeper.mobile.ui.components.StatCard
import com.gatekeeper.mobile.ui.navigation.Screen
import com.gatekeeper.mobile.ui.navigation.safeNavigate
import com.gatekeeper.mobile.ui.theme.*

enum class ProtectModule(
    val label: String,
    val color: androidx.compose.ui.graphics.Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val subtitle: String,
    val route: String
) {
    APP_GATE("App Gate", ModuleAppGate, Icons.Filled.Policy, "Control app internet access", Screen.Firewall.route),
    WEB_GATE("Web Gate", ModuleWebGate, Icons.Filled.Wifi, "Block malicious domains", Screen.DnsFilter.route),
    THREAT_INTEL("Threat Intel", ModuleThreatIntel, Icons.Filled.Security, "Live threat intelligence", Screen.ThreatFeed.route),
    WIFI_GUARD("Wi-Fi Guard", ModuleWifiGuard, Icons.Filled.WifiTethering, "Wireless security monitor", Screen.WifiScanner.route),
    TRUST_CHECK("Trust Check", ModuleTrustCheck, Icons.Filled.VerifiedUser, "SSL certificate audit", Screen.CertAudit.route),
    PRIVACY_SCAN("Privacy Scan", ModulePrivacyScan, Icons.Filled.Lock, "App permission audit", Screen.PermissionAuditor.route)
}

@Composable
fun ProtectHubScreen(navController: NavController) {
    var selectedModule by remember { mutableStateOf(ProtectModule.APP_GATE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassAmbientBackground()
    ) {
        // ── Top app bar ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = LocalGKColors.current.primary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Protect",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LocalGKColors.current.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Module chip strip ─────────────────────────────────────────────────
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ProtectModule.values()) { module ->
                val isSelected = module == selectedModule
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedModule = module },
                    label = {
                        Text(
                            module.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(module.icon, null, modifier = Modifier.size(16.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = module.color,
                        selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                        selectedLeadingIconColor = androidx.compose.ui.graphics.Color.White,
                        containerColor = module.color.copy(alpha = 0.08f),
                        labelColor = module.color,
                        iconColor = module.color
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = module.color.copy(alpha = 0.4f),
                        selectedBorderColor = module.color
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Animated module content ───────────────────────────────────────────
        AnimatedContent(
            targetState = selectedModule,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ProtectModuleContent"
        ) { module ->
            ModuleContentView(module = module, navController = navController)
        }
    }
}

@Composable
private fun ModuleContentView(module: ProtectModule, navController: NavController) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            // Module header card — uses GKModuleHeader (Step 3)
            GKModuleHeader(
                title = module.label,
                subtitle = module.subtitle,
                icon = module.icon,
                moduleColor = module.color,
                isEnabled = true,
                onToggle = { /* viewModel toggling is done in full screen */ }
            )
        }
        item {
            // Stats row — 2 StatCards
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    label = "Blocked",
                    value = "—",
                    gradientColors = listOf(LocalGKColors.current.accentRed, LocalGKColors.current.accentRed.copy(alpha = 0.7f)),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Allowed",
                    value = "—",
                    gradientColors = listOf(LocalGKColors.current.accentGreen, LocalGKColors.current.accentGreen.copy(alpha = 0.7f)),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            SectionHeader(title = "Recent Activity")
        }
        items(3) { index ->
            GKListRow(
                icon = module.icon,
                iconTint = module.color,
                title = "No recent data — tap View All",
                subtitle = "See full module for details"
            )
        }
        item {
            Spacer(Modifier.height(4.dp))
            GKPrimaryButton(
                text = "View All — ${module.label}",
                onClick = { navController.safeNavigate(module.route) }
            )
            Spacer(Modifier.height(80.dp)) // bottom nav clearance
        }
    }
}
