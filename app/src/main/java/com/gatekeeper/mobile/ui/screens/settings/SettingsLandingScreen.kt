package com.gatekeeper.mobile.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.components.HtmlToggleSwitch
import com.gatekeeper.mobile.ui.theme.*

@Composable
fun SettingsLandingScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isScreenOffBlockingEnabled by viewModel.isScreenOffBlockingEnabled.collectAsState()
    val isDnsLeakProtectionEnabled by viewModel.isDnsLeakProtectionEnabled.collectAsState()
    val isFirewallBypassDetectEnabled by viewModel.isFirewallBypassDetectEnabled.collectAsState()
    val isDnsExfilDetectionEnabled by viewModel.isDnsExfilDetectionEnabled.collectAsState()
    
    var desktopIp by remember { mutableStateOf("192.168.1.100") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .glassAmbientBackground()
            .verticalScroll(rememberScrollState())
    ) {
        // TopAppBar equivalent in the HTML
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shield, null, tint = LocalGKColors.current.primary)
                Spacer(Modifier.width(8.dp))
                Text("GateKeeper", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LocalGKColors.current.primary)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // Header
            Text("Settings", style = MaterialTheme.typography.headlineLarge, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.Bold)
            Text("Configure your protection parameters.", style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textSecondary)
            
            Spacer(Modifier.height(24.dp))

            // Protection Section
            SectionHeaderWithIcon(icon = Icons.Outlined.Security, title = "Protection")
            HtmlGlassCard {
                SettingsRowSwitch(
                    title = "VPN auto-start",
                    subtitle = "Connect automatically on untrusted networks.",
                    checked = isScreenOffBlockingEnabled, // Map to what we have
                    onCheckedChange = { viewModel.setScreenOffBlocking(it) }
                )
                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                SettingsRowSwitch(
                    title = "DNS leak protection",
                    subtitle = "Force all DNS requests through secure tunnel.",
                    checked = isDnsLeakProtectionEnabled,
                    onCheckedChange = { viewModel.setDnsLeakProtection(it) }
                )
                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                SettingsRowSwitch(
                    title = "SafeSearch",
                    subtitle = "Filter malicious domains at the network level.",
                    checked = isFirewallBypassDetectEnabled,
                    onCheckedChange = { viewModel.setFirewallBypassDetect(it) }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Privacy Section
            SectionHeaderWithIcon(icon = Icons.Outlined.VisibilityOff, title = "Privacy")
            HtmlGlassCard {
                SettingsRowSwitch(
                    title = "IMSI detection",
                    subtitle = "Alert on suspicious cell tower handoffs.",
                    checked = true,
                    onCheckedChange = { }
                )
                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                SettingsRowSwitch(
                    title = "Background sensor alerts",
                    subtitle = "Notify when apps access mic/camera silently.",
                    checked = true,
                    onCheckedChange = { }
                )
                Divider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                SettingsRowSwitch(
                    title = "Evil twin detection",
                    subtitle = "Warn about spoofed Wi-Fi access points.",
                    checked = isDnsExfilDetectionEnabled,
                    onCheckedChange = { viewModel.setDnsExfilDetection(it) }
                )
            }

            Spacer(Modifier.height(24.dp))



            // About Section
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 100.dp).padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Shield, null, modifier = Modifier.size(36.dp), tint = LocalGKColors.current.textSecondary.copy(alpha = 0.7f))
                Spacer(Modifier.height(8.dp))
                Text("GateKeeper Security", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textPrimary.copy(alpha = 0.7f))
                Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary.copy(alpha = 0.7f))
                Spacer(Modifier.height(16.dp))
                Text(
                    "* Some advanced features may be limited depending on device root access and hardware capabilities.",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalGKColors.current.textSecondary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeaderWithIcon(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = LocalGKColors.current.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = LocalGKColors.current.primary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun HtmlGlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        content()
    }
}

@Composable
fun SettingsRowSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textPrimary)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Info, null, modifier = Modifier.size(16.dp), tint = LocalGKColors.current.primary.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
        }
        HtmlToggleSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            activeColor = LocalGKColors.current.primary
        )
    }
}

@Composable
fun SettingsActionRow(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textPrimary)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Info, null, modifier = Modifier.size(16.dp), tint = LocalGKColors.current.primary.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
        }
        Icon(icon, null, tint = LocalGKColors.current.primary, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun SettingsThemeChip(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) LocalGKColors.current.primary.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) LocalGKColors.current.primary else LocalGKColors.current.textSecondary,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
