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
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(TextTertiary.copy(alpha = 0.08f), DarkBackground)))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            if (navController != null) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(PrimaryCyan.copy(alpha = 0.2f), PrimaryBlue.copy(alpha = 0.2f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Settings, null, tint = PrimaryCyan, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Settings", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                    Text("GateKeeper Security Configuration", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
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
                    tint = AccentOrange,
                    checked = isScreenOffBlockingEnabled,
                    onCheckedChange = { viewModel.setScreenOffBlocking(it) }
                )
                SettingsDivider()
                SettingsSwitch(
                    icon = Icons.Filled.GppBad,
                    title = "DNS Leak Prevention",
                    subtitle = "Block apps using DNS-over-HTTPS to bypass the DNS filter (e.g., 1.1.1.1, 8.8.8.8 on port 443)",
                    tint = PrimaryCyan,
                    checked = isDnsLeakProtectionEnabled,
                    onCheckedChange = { viewModel.setDnsLeakProtection(it) }
                )
                SettingsDivider()
                SettingsSwitch(
                    icon = Icons.Filled.BugReport,
                    title = "Firewall Bypass Detection",
                    subtitle = "Alert when blocked apps connect using hardcoded IPs (no DNS lookup)",
                    tint = AccentRed,
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
                    tint = AccentYellow,
                    checked = isDnsExfilDetectionEnabled,
                    onCheckedChange = { viewModel.setDnsExfilDetection(it) }
                )
                SettingsDivider()
                SettingsSwitch(
                    icon = Icons.Filled.Mic,
                    title = "Background Sensor Alerts",
                    subtitle = "Alert when apps access Camera, Microphone, or Location while running in background",
                    tint = AccentOrange,
                    checked = isBackgroundSensorAlertsEnabled,
                    onCheckedChange = { viewModel.setBackgroundSensorAlerts(it) }
                )
                SettingsDivider()
                SettingsSwitch(
                    icon = Icons.Filled.NoPhotography,
                    title = "Global Camera Block",
                    subtitle = "Blocks camera access for all apps system-wide (requires Device Admin)",
                    tint = AccentRed,
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
                    tint = AccentRed,
                    checked = isImsiDetectionEnabled,
                    onCheckedChange = { viewModel.setImsiDetection(it) }
                )
                SettingsDivider()
                SettingsSwitch(
                    icon = Icons.Filled.Wifi,
                    title = "Evil Twin AP Detection",
                    subtitle = "Detect duplicate Wi-Fi networks that may be rogue access points performing MITM",
                    tint = AccentYellow,
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
                    tint = AccentOrange,
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
                    tint = AccentGreen,
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
                    tint = AccentGreen,
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
                    tint = PrimaryCyan,
                    onClick = { tempIp = backendIp; showIpDialog = true }
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Feature Matrix Banner ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(PrimaryCyan.copy(alpha = 0.05f), PrimaryBlue.copy(alpha = 0.07f))))
                    .padding(16.dp)
            ) {
                Text("🛡️  GateKeeper Security Feature Matrix", style = MaterialTheme.typography.titleMedium, color = PrimaryCyan, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                val features = listOf(
                    "✅ VPN Split-Tunnel (per-app firewall)" to PrimaryCyan,
                    "✅ DNS Sinkhole (domain-level blocking)" to PrimaryCyan,
                    "✅ DNS-over-HTTPS Leak Prevention" to PrimaryCyan,
                    "✅ DNS Exfiltration (Shannon Entropy)" to PrimaryCyan,
                    "✅ Firewall Bypass Detection (IP direct)" to PrimaryCyan,
                    "✅ Screen-Off Per-App Blocking" to AccentGreen,
                    "✅ IMSI Catcher / 2G Downgrade Alert" to AccentGreen,
                    "✅ Evil Twin AP Detection" to AccentGreen,
                    "✅ Rogue SSL Certificate Auditor" to AccentGreen,
                    "✅ Real-time Sensor Access Logging" to AccentGreen,
                    "✅ Background Camera/Mic/Location Alert" to AccentGreen,
                    "✅ DNS Blocklist Subscriptions" to AccentYellow,
                    "✅ GeoIP Country Blocking" to AccentYellow,
                    "✅ PCAP Packet Capture (Wireshark)" to AccentYellow,
                    "✅ Threat Intelligence Feed Import" to AccentYellow,
                    "✅ AI Security Assistant" to AccentOrange,
                    "✅ Traffic Monitor (per-app)" to AccentOrange,
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
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Final Year Project — Mobile Network Security Suite\nCore engine powered by Android VpnService API",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showIpDialog) {
        AlertDialog(
            onDismissRequest = { showIpDialog = false },
            containerColor = DarkSurfaceVariant,
            title = { Text("Configure Backend IP", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Enter the IPv4 address of your PC running GateKeeper Desktop (e.g. 192.168.1.100). This enables bidirectional threat intelligence sync.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempIp,
                        onValueChange = { tempIp = it },
                        label = { Text("IP Address", color = TextTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCyan,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.setBackendIp(tempIp.trim()); showIpDialog = false }) {
                    Text("Save", color = PrimaryCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIpDialog = false }) {
                    Text("Cancel", color = TextSecondary)
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
        color = PrimaryCyan,
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
            .background(DarkCard),
        content = content
    )
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = GlassBorder,
        thickness = 0.5.dp
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = PrimaryCyan,
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
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (isDestructive) AccentRed else TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = MaterialTheme.typography.bodySmall.lineHeight)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = PrimaryCyan,
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
            Icon(icon, null, tint = if (checked) tint else TextTertiary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = MaterialTheme.typography.bodySmall.lineHeight)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = tint,
                checkedTrackColor = tint.copy(alpha = 0.3f),
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}
