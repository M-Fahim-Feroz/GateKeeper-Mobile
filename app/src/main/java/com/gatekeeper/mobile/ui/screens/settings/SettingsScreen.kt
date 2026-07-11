package com.gatekeeper.mobile.ui.screens.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.theme.*
import com.gatekeeper.mobile.ui.components.GKInfoDialog
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val backendIp by viewModel.backendIp.collectAsState()
    val isPcapCaptureEnabled by viewModel.isPcapCaptureEnabled.collectAsState()
    val isDnsLeakProtectionEnabled by viewModel.isDnsLeakProtectionEnabled.collectAsState()
    val isDnsExfilDetectionEnabled by viewModel.isDnsExfilDetectionEnabled.collectAsState()
    val isScreenOffBlockingEnabled by viewModel.isScreenOffBlockingEnabled.collectAsState()
    val isImsiDetectionEnabled by viewModel.isImsiDetectionEnabled.collectAsState()
    val isBackgroundSensorAlertsEnabled by viewModel.isBackgroundSensorAlertsEnabled.collectAsState()
    val isEvilTwinDetectionEnabled by viewModel.isEvilTwinDetectionEnabled.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    var showIpDialog by remember { mutableStateOf(false) }
    var tempIp by remember { mutableStateOf("") }
    var showDnsLeakInfo by remember { mutableStateOf(false) }
    var showImsiInfo by remember { mutableStateOf(false) }
    var showSensorInfo by remember { mutableStateOf(false) }
    var showEvilTwinInfo by remember { mutableStateOf(false) }
    var showVpnAutoStartInfo by remember { mutableStateOf(false) }
    var showSafeSearchInfo by remember { mutableStateOf(false) }


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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController?.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LocalGKColors.current.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LocalGKColors.current.primary)
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // Header
            Column(modifier = Modifier.padding(vertical = 24.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Configure your protection parameters.", style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textSecondary)
            }

            // Protection Section
            SettingsSectionTitle(Icons.Filled.Security, "Protection")
            SettingsCard {
                SettingsToggleItem(
                    title = "VPN auto-start",
                    subtitle = "Connect automatically on untrusted networks.",
                    checked = isScreenOffBlockingEnabled, // Mapped to existing pref
                    onCheckedChange = { viewModel.setScreenOffBlocking(it) },
                    showDivider = true,
                    onInfoClick = { showVpnAutoStartInfo = true }
                )
                SettingsToggleItem(
                    title = "DNS leak protection",
                    subtitle = "Force all DNS requests through secure tunnel.",
                    checked = isDnsLeakProtectionEnabled,
                    onCheckedChange = { viewModel.setDnsLeakProtection(it) },
                    showDivider = true,
                    onInfoClick = { showDnsLeakInfo = true }
                )
                SettingsToggleItem(
                    title = "SafeSearch",
                    subtitle = "Filter malicious domains at the network level.",
                    checked = isDnsExfilDetectionEnabled, // Mapped to existing pref
                    onCheckedChange = { viewModel.setDnsExfilDetection(it) },
                    showDivider = false,
                    onInfoClick = { showSafeSearchInfo = true }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Privacy Section
            SettingsSectionTitle(Icons.Filled.VisibilityOff, "Privacy")
            SettingsCard {
                SettingsToggleItem(
                    title = "IMSI detection",
                    subtitle = "Alert on suspicious cell tower handoffs.",
                    checked = isImsiDetectionEnabled,
                    onCheckedChange = { viewModel.setImsiDetection(it) },
                    showDivider = true,
                    onInfoClick = { showImsiInfo = true }
                )
                SettingsToggleItem(
                    title = "Background sensor alerts",
                    subtitle = "Notify when apps access mic/camera silently.",
                    checked = isBackgroundSensorAlertsEnabled,
                    onCheckedChange = { viewModel.setBackgroundSensorAlerts(it) },
                    showDivider = true,
                    onInfoClick = { showSensorInfo = true }
                )
                SettingsToggleItem(
                    title = "Evil twin detection",
                    subtitle = "Warn about spoofed Wi-Fi access points.",
                    checked = isEvilTwinDetectionEnabled,
                    onCheckedChange = { viewModel.setEvilTwinDetection(it) },
                    showDivider = false,
                    onInfoClick = { showEvilTwinInfo = true }
                )
            }



            // About Section
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Shield, null, tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("GateKeeper Security", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textPrimary)
                Text("Version 1.0.0", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "* Some advanced features may be limited depending on device root access and hardware capabilities.",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalGKColors.current.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.width(280.dp)
                )
            }
        }
    }

    if (showDnsLeakInfo) {
        GKInfoDialog(
            title = "DNS Leak Protection",
            body = "When you type a web address, your device first asks a DNS server where to find that website. Without protection, this request can leak outside the secure tunnel, revealing which sites you're visiting.\n\nDNS Leak Protection forces all these requests to go through GateKeeper's secure channel so your browsing stays private.",
            onDismiss = { showDnsLeakInfo = false }
        )
    }
    if (showImsiInfo) {
        GKInfoDialog(
            title = "IMSI Detection",
            body = "Your phone has a unique identity number called an IMSI. Attackers can use fake cell towers (also called Stingrays or IMSI catchers) to intercept your calls and messages.\n\nIMSI Detection watches for unusual cell tower handoffs that may indicate a fake tower nearby and alerts you immediately.",
            onDismiss = { showImsiInfo = false }
        )
    }
    if (showSensorInfo) {
        GKInfoDialog(
            title = "Background Sensor Alerts",
            body = "Some apps secretly access your microphone, camera, or location while running in the background — without any visible indicator.\n\nWith this enabled, GateKeeper will notify you whenever an app accesses these sensitive sensors while you're not actively using it.",
            onDismiss = { showSensorInfo = false }
        )
    }
    if (showEvilTwinInfo) {
        GKInfoDialog(
            title = "Evil Twin Detection",
            body = "An Evil Twin is a fake Wi-Fi hotspot that mimics a real network name to trick your device into connecting. Once connected, the attacker can see everything you send and receive.\n\nThis feature continuously watches for networks that match known ones but have a different identity, and warns you before you can be tricked.",
            onDismiss = { showEvilTwinInfo = false }
        )
    }
    if (showVpnAutoStartInfo) {
        GKInfoDialog(
            title = "VPN Auto-Start",
            body = "Automatically turns on GateKeeper's protection whenever your device starts up or connects to an untrusted Wi-Fi network.\n\nThis ensures you're never accidentally browsing without protection.",
            onDismiss = { showVpnAutoStartInfo = false }
        )
    }
    if (showSafeSearchInfo) {
        GKInfoDialog(
            title = "SafeSearch",
            body = "Automatically intercepts and blocks connections to websites known to host malware, trackers, or adult content.\n\nIt works across all your apps and browsers silently in the background.",
            onDismiss = { showSafeSearchInfo = false }
        )
    }

}

@Composable
fun ThemeButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (isSelected) LocalGKColors.current.primary.copy(alpha = 0.2f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) LocalGKColors.current.primary else LocalGKColors.current.textSecondary,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun SettingsSectionTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
        Icon(icon, null, tint = LocalGKColors.current.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = LocalGKColors.current.primary)
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        // Subtle inner glow simulating the CSS
        Box(modifier = Modifier.fillMaxSize().background(Brush.radialGradient(listOf(LocalGKColors.current.primary.copy(alpha = 0.05f), Color.Transparent), radius = 300f)))
        Column(modifier = Modifier.padding(24.dp), content = content)
    }
}

@Composable
fun SettingsToggleItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, showDivider: Boolean, onInfoClick: (() -> Unit)? = null) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textPrimary)
                    if (onInfoClick != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onInfoClick, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Info, null, tint = LocalGKColors.current.primary.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary, lineHeight = 16.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = LocalGKColors.current.primary,
                    uncheckedThumbColor = LocalGKColors.current.textSecondary,
                    uncheckedTrackColor = LocalGKColors.current.surfaceVariant
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun SettingsActionItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, showDivider: Boolean) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = { /* Not used directly but present in layout */ }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Info, null, tint = LocalGKColors.current.primary.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary, lineHeight = 16.sp)
            }
            Icon(icon, null, tint = LocalGKColors.current.primary, modifier = Modifier.size(24.dp))
        }
        if (showDivider) {
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}
