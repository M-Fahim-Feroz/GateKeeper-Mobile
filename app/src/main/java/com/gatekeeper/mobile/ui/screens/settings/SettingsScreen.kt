package com.gatekeeper.mobile.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.content.Context
import android.widget.Toast
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.theme.*
import com.gatekeeper.mobile.receiver.GateKeeperDeviceAdminReceiver
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    navController: NavController? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val devicePolicyManager = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, GateKeeperDeviceAdminReceiver::class.java) }

    val backendIp by viewModel.backendIp.collectAsState()
    val isPcapCaptureEnabled by viewModel.isPcapCaptureEnabled.collectAsState()
    val isDnsLeakProtectionEnabled by viewModel.isDnsLeakProtectionEnabled.collectAsState()
    val isDnsExfilDetectionEnabled by viewModel.isDnsExfilDetectionEnabled.collectAsState()
    val isScreenOffBlockingEnabled by viewModel.isScreenOffBlockingEnabled.collectAsState()
    val isImsiDetectionEnabled by viewModel.isImsiDetectionEnabled.collectAsState()
    val isFirewallBypassDetectEnabled by viewModel.isFirewallBypassDetectEnabled.collectAsState()
    val isBackgroundSensorAlertsEnabled by viewModel.isBackgroundSensorAlertsEnabled.collectAsState()
    val isGlobalCameraBlockEnabled by viewModel.isGlobalCameraBlockEnabled.collectAsState()
    val isEvilTwinDetectionEnabled by viewModel.isEvilTwinDetectionEnabled.collectAsState()

    var showIpDialog by remember { mutableStateOf(false) }
    var tempIp by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalGKColors.current.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(LocalGKColors.current.textTertiary.copy(alpha = 0.08f), LocalGKColors.current.background)))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            if (navController != null) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(LocalGKColors.current.primary.copy(alpha = 0.2f), PrimaryBlue.copy(alpha = 0.2f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Settings, null, tint = LocalGKColors.current.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Settings", style = MaterialTheme.typography.displaySmall, color = LocalGKColors.current.textPrimary)
                    Text("GateKeeper Security Configuration", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

            // ── VPN & Network ───────────────────────────────────────────────
            SettingsSectionLabel("VPN & Network Protection")
            SettingsCard {
                SettingsSwitch(
                    icon = Icons.Filled.NightlightRound,
                    title = "Screen-Off Network Blocking",
                    subtitle = "Block apps you've selected when screen turns off. Per-app in Firewall.",
                    tint = LocalGKColors.current.accentOrange,
                    checked = isScreenOffBlockingEnabled,
                    onCheckedChange = { viewModel.setScreenOffBlocking(it) }
                )
                SettingsDivider()
                SettingsSwitch(
                    icon = Icons.Filled.GppBad,
                    title = "DNS Leak Prevention",
                    subtitle = "Block apps using DNS-over-HTTPS to bypass the DNS filter (e.g., 1.1.1.1, 8.8.8.8 on port 443)",
                    tint = LocalGKColors.current.primary,
                    checked = isDnsLeakProtectionEnabled,
                    onCheckedChange = { viewModel.setDnsLeakProtection(it) }
                )
                SettingsDivider()
                SettingsSwitch(
                    icon = Icons.Filled.BugReport,
                    title = "Firewall Bypass Detection",
                    subtitle = "Alert when blocked apps connect using hardcoded IPs (no DNS lookup)",
                    tint = LocalGKColors.current.accentRed,
                    checked = isFirewallBypassDetectEnabled,
                    onCheckedChange = { viewModel.setFirewallBypassDetect(it) }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Privacy & Sensor Monitoring ─────────────────────────────────
            SettingsSectionLabel("Privacy & Sensor Monitoring")
            SettingsCard {
                SettingsSwitch(
                    icon = Icons.Filled.Sensors,
                    title = "DNS Exfiltration Detection",
                    subtitle = "Flag apps with high-entropy DNS queries (Shannon analysis) — possible data exfiltration via DNS tunneling",
                    tint = LocalGKColors.current.accentYellow,
                    checked = isDnsExfilDetectionEnabled,
                    onCheckedChange = { viewModel.setDnsExfilDetection(it) }
                )
                SettingsDivider()
                SettingsSwitch(
                    icon = Icons.Filled.Mic,
                    title = "Background Sensor Alerts",
                    subtitle = "Alert when apps access Camera, Microphone, or Location while running in background",
                    tint = LocalGKColors.current.accentOrange,
                    checked = isBackgroundSensorAlertsEnabled,
                    onCheckedChange = { viewModel.setBackgroundSensorAlerts(it) }
                )
                SettingsDivider()
                SettingsSwitch(
                    icon = Icons.Filled.NoPhotography,
                    title = "Global Camera Block",
                    subtitle = "Blocks camera access for all apps system-wide (requires Device Admin)",
                    tint = LocalGKColors.current.accentRed,
                    checked = isGlobalCameraBlockEnabled,
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            if (!devicePolicyManager.isAdminActive(adminComponent)) {
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                    putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "GateKeeper needs Device Admin privileges to block the camera system-wide.")
                                }
                                context.startActivity(intent)
                            } else {
                                try {
                                    devicePolicyManager.setCameraDisabled(adminComponent, true)
                                    viewModel.setGlobalCameraBlock(true)
                                } catch (e: SecurityException) {
                                    Toast.makeText(context, "Error disabling camera: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            if (devicePolicyManager.isAdminActive(adminComponent)) {
                                try {
                                    devicePolicyManager.setCameraDisabled(adminComponent, false)
                                } catch (e: SecurityException) {
                                    // Ignore if already disabled or permission removed
                                }
                            }
                            viewModel.setGlobalCameraBlock(false)
                        }
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Threat Detection ────────────────────────────────────────────
            SettingsSectionLabel("Threat Detection Engines")
            SettingsCard {
                SettingsSwitch(
                    icon = Icons.Filled.CellTower,
                    title = "IMSI Catcher Detection",
                    subtitle = "Alert on 4G→2G cellular downgrade — possible fake tower (Stingray) nearby",
                    tint = LocalGKColors.current.accentRed,
                    checked = isImsiDetectionEnabled,
                    onCheckedChange = { viewModel.setImsiDetection(it) }
                )
                SettingsDivider()
                SettingsSwitch(
                    icon = Icons.Filled.Wifi,
                    title = "Evil Twin AP Detection",
                    subtitle = "Detect duplicate Wi-Fi networks that may be rogue access points performing MITM",
                    tint = LocalGKColors.current.accentYellow,
                    checked = isEvilTwinDetectionEnabled,
                    onCheckedChange = { viewModel.setEvilTwinDetection(it) }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Advanced / Forensics ────────────────────────────────────────
            SettingsSectionLabel("Advanced & Forensics")
            SettingsCard {
                SettingsItem(
                    icon = Icons.Filled.Notifications,
                    title = "Notification Preferences",
                    subtitle = "Manage security alerts, heads-up notifications, and sound",
                    tint = LocalGKColors.current.accentOrange,
                    onClick = {
                        val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    }
                )
                SettingsDivider()
                SettingsSwitch(
                    icon = Icons.Filled.Policy,
                    title = "PCAP Traffic Capture",
                    subtitle = "Record all raw packets to .pcap file for Wireshark analysis",
                    tint = PrimaryBlue,
                    checked = isPcapCaptureEnabled,
                    onCheckedChange = { viewModel.setPcapCaptureEnabled(it) }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Filled.FileDownload,
                    title = "Export Traffic Logs",
                    subtitle = "Save connection logs as CSV for analysis",
                    tint = LocalGKColors.current.accentGreen,
                    onClick = {
                        scope.launch {
                            val result = viewModel.exportTrafficLogs(context)
                            val msg = if (result.isSuccess) "Exported to ${result.getOrNull()?.name}" else "Export failed: ${result.exceptionOrNull()?.message}"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                )
                SettingsDivider()
                SettingsItem(
                    icon = Icons.Filled.Rule,
                    title = "Export Firewall Rules",
                    subtitle = "Save current firewall rules as JSON",
                    tint = LocalGKColors.current.accentGreen,
                    onClick = {
                        scope.launch {
                            val result = viewModel.exportRules(context)
                            val msg = if (result.isSuccess) "Exported to ${result.getOrNull()?.name}" else "Export failed: ${result.exceptionOrNull()?.message}"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            // ── Desktop Integration ─────────────────────────────────────────
            SettingsSectionLabel("GateKeeper Desktop Integration")
            SettingsCard {
                SettingsItem(
                    icon = Icons.Filled.Computer,
                    title = "Backend IP Address",
                    subtitle = backendIp.ifEmpty { "Not configured — tap to set" },
                    tint = LocalGKColors.current.primary,
                    onClick = { tempIp = backendIp; showIpDialog = true }
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Feature Matrix Banner ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(LocalGKColors.current.primary.copy(alpha = 0.05f), PrimaryBlue.copy(alpha = 0.07f))))
                    .padding(16.dp)
            ) {
                Text("🛡️  GateKeeper Security Feature Matrix", style = MaterialTheme.typography.titleMedium, color = LocalGKColors.current.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                val features = listOf(
                    "✅ VPN Split-Tunnel (per-app firewall)" to LocalGKColors.current.primary,
                    "✅ DNS Sinkhole (domain-level blocking)" to LocalGKColors.current.primary,
                    "✅ DNS-over-HTTPS Leak Prevention" to LocalGKColors.current.primary,
                    "✅ DNS Exfiltration (Shannon Entropy)" to LocalGKColors.current.primary,
                    "✅ Firewall Bypass Detection (IP direct)" to LocalGKColors.current.primary,
                    "✅ Screen-Off Per-App Blocking" to LocalGKColors.current.accentGreen,
                    "✅ IMSI Catcher / 2G Downgrade Alert" to LocalGKColors.current.accentGreen,
                    "✅ Evil Twin AP Detection" to LocalGKColors.current.accentGreen,
                    "✅ Rogue SSL Certificate Auditor" to LocalGKColors.current.accentGreen,
                    "✅ Real-time Sensor Access Logging" to LocalGKColors.current.accentGreen,
                    "✅ Background Camera/Mic/Location Alert" to LocalGKColors.current.accentGreen,
                    "✅ DNS Blocklist Subscriptions" to LocalGKColors.current.accentYellow,
                    "✅ GeoIP Country Blocking" to LocalGKColors.current.accentYellow,
                    "✅ PCAP Packet Capture (Wireshark)" to LocalGKColors.current.accentYellow,
                    "✅ Threat Intelligence Feed Import" to LocalGKColors.current.accentYellow,
                    "✅ AI Security Assistant" to LocalGKColors.current.accentOrange,
                    "✅ Traffic Monitor (per-app)" to LocalGKColors.current.accentOrange,
                )
                features.forEach { (text, color) ->
                    Row(modifier = Modifier.padding(vertical = 3.dp)) {
                        Text(text, style = MaterialTheme.typography.bodySmall, color = color)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // About
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "GateKeeper Mobile v1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalGKColors.current.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Final Year Project — Mobile Network Security Suite\nCore engine powered by Android VpnService API",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalGKColors.current.textTertiary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showIpDialog) {
        AlertDialog(
            onDismissRequest = { showIpDialog = false },
            containerColor = LocalGKColors.current.surfaceVariant,
            title = { Text("Configure Backend IP", color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Enter the IPv4 address of your PC running GateKeeper Desktop (e.g. 192.168.1.100). This enables bidirectional threat intelligence sync.",
                        color = LocalGKColors.current.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempIp,
                        onValueChange = { tempIp = it },
                        label = { Text("IP Address", color = LocalGKColors.current.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LocalGKColors.current.primary,
                            unfocusedBorderColor = LocalGKColors.current.border,
                            focusedTextColor = LocalGKColors.current.textPrimary,
                            unfocusedTextColor = LocalGKColors.current.textPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setBackendIp(tempIp.trim()); showIpDialog = false }) {
                    Text("Save", color = LocalGKColors.current.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIpDialog = false }) {
                    Text("Cancel", color = LocalGKColors.current.textSecondary)
                }
            }
        )
    }
}

// ─── Reusable Settings composables ──────────────────────────────────────────

@Composable
fun SettingsSectionLabel(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleSmall,
        color = LocalGKColors.current.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LocalGKColors.current.card),
        content = content
    )
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = LocalGKColors.current.border,
        thickness = 0.5.dp
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = LocalGKColors.current.primary,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (isDestructive) LocalGKColors.current.accentRed else LocalGKColors.current.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary, lineHeight = MaterialTheme.typography.bodySmall.lineHeight)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = LocalGKColors.current.textTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = LocalGKColors.current.primary,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(9.dp))
                .background(tint.copy(alpha = if (checked) 0.18f else 0.08f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (checked) tint else LocalGKColors.current.textTertiary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary, lineHeight = MaterialTheme.typography.bodySmall.lineHeight)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = tint,
                checkedTrackColor = tint.copy(alpha = 0.3f),
                uncheckedThumbColor = LocalGKColors.current.textTertiary,
                uncheckedTrackColor = LocalGKColors.current.surfaceVariant
            )
        )
    }
}
