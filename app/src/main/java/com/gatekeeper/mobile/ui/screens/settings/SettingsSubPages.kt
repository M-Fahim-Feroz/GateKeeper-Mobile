package com.gatekeeper.mobile.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.receiver.GateKeeperDeviceAdminReceiver
import com.gatekeeper.mobile.ui.theme.*
import kotlinx.coroutines.launch

// ─── Sub-page: Protection ────────────────────────────────────────────────────

@Composable
fun SettingsProtectionScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isScreenOffBlockingEnabled by viewModel.isScreenOffBlockingEnabled.collectAsState()
    val isDnsLeakProtectionEnabled by viewModel.isDnsLeakProtectionEnabled.collectAsState()
    val isFirewallBypassDetectEnabled by viewModel.isFirewallBypassDetectEnabled.collectAsState()

    SettingsSubPage(title = "Protection", subtitle = "Network blocking & leak prevention", navController = navController) {
        SettingsCard {
            SettingsSwitch(
                icon = Icons.Filled.NightlightRound,
                title = "Block apps when screen is off",
                subtitle = "Selected apps can't access the internet while your screen is off",
                tint = LocalGKColors.current.accentOrange,
                checked = isScreenOffBlockingEnabled,
                onCheckedChange = { viewModel.setScreenOffBlocking(it) }
            )
            SettingsDivider()
            SettingsSwitch(
                icon = Icons.Filled.GppBad,
                title = "DNS privacy guard",
                subtitle = "Stops apps from sneaking past the DNS filter using encrypted DNS",
                tint = LocalGKColors.current.primary,
                checked = isDnsLeakProtectionEnabled,
                onCheckedChange = { viewModel.setDnsLeakProtection(it) }
            )
            SettingsDivider()
            SettingsSwitch(
                icon = Icons.Filled.BugReport,
                title = "Bypass attempt detector",
                subtitle = "Alerts you if a blocked app tries to connect using a hardcoded server address",
                tint = LocalGKColors.current.accentRed,
                checked = isFirewallBypassDetectEnabled,
                onCheckedChange = { viewModel.setFirewallBypassDetect(it) }
            )
            SettingsDivider()
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
        }
    }
}

// ─── Sub-page: Privacy ───────────────────────────────────────────────────────

@Composable
fun SettingsPrivacyScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val devicePolicyManager = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }
    val adminComponent = remember { ComponentName(context, GateKeeperDeviceAdminReceiver::class.java) }

    val isDnsExfilDetectionEnabled by viewModel.isDnsExfilDetectionEnabled.collectAsState()
    val isBackgroundSensorAlertsEnabled by viewModel.isBackgroundSensorAlertsEnabled.collectAsState()
    val isGlobalCameraBlockEnabled by viewModel.isGlobalCameraBlockEnabled.collectAsState()
    val isImsiDetectionEnabled by viewModel.isImsiDetectionEnabled.collectAsState()
    val isEvilTwinDetectionEnabled by viewModel.isEvilTwinDetectionEnabled.collectAsState()

    SettingsSubPage(title = "Privacy", subtitle = "Sensor access, camera & threat detection", navController = navController) {
        SettingsCard {
            SettingsSwitch(
                icon = Icons.Filled.Sensors,
                title = "Secret data leak detector",
                subtitle = "Alerts if an app tunnels data through DNS queries",
                tint = LocalGKColors.current.accentYellow,
                checked = isDnsExfilDetectionEnabled,
                onCheckedChange = { viewModel.setDnsExfilDetection(it) }
            )
            SettingsDivider()
            SettingsSwitch(
                icon = Icons.Filled.Mic,
                title = "Background camera & mic alerts",
                subtitle = "Notifies you when apps access your camera or mic while running in background",
                tint = LocalGKColors.current.accentOrange,
                checked = isBackgroundSensorAlertsEnabled,
                onCheckedChange = { viewModel.setBackgroundSensorAlerts(it) }
            )
            SettingsDivider()
            SettingsSwitch(
                icon = Icons.Filled.NoPhotography,
                title = "Global camera block",
                subtitle = "Completely disables the camera for all apps (requires Device Admin)",
                tint = LocalGKColors.current.accentRed,
                checked = isGlobalCameraBlockEnabled,
                onCheckedChange = { isChecked ->
                    if (isChecked) {
                        if (!devicePolicyManager.isAdminActive(adminComponent)) {
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "GateKeeper needs Device Admin to block the camera system-wide.")
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
                            try { devicePolicyManager.setCameraDisabled(adminComponent, false) } catch (_: SecurityException) {}
                        }
                        viewModel.setGlobalCameraBlock(false)
                    }
                }
            )
        }

        AnimatedVisibility(visible = isGlobalCameraBlockEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LocalGKColors.current.accentOrange.copy(alpha = 0.08f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Warning, null, tint = LocalGKColors.current.accentOrange, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "⚠️ This disables the camera for ALL apps including the camera app",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalGKColors.current.accentOrange
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsCard {
            SettingsSwitch(
                icon = Icons.Filled.CellTower,
                title = "Fake cell tower detector",
                subtitle = "Warns you if a fake cell tower is trying to intercept your calls",
                tint = LocalGKColors.current.accentRed,
                checked = isImsiDetectionEnabled,
                onCheckedChange = { viewModel.setImsiDetection(it) }
            )
            SettingsDivider()
            SettingsSwitch(
                icon = Icons.Filled.Wifi,
                title = "Fake Wi-Fi detector",
                subtitle = "Detects duplicate Wi-Fi networks that may be trying to steal your connection",
                tint = LocalGKColors.current.accentYellow,
                checked = isEvilTwinDetectionEnabled,
                onCheckedChange = { viewModel.setEvilTwinDetection(it) }
            )
        }
    }
}

// ─── Sub-page: Advanced ──────────────────────────────────────────────────────

@Composable
fun SettingsAdvancedScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPcapCaptureEnabled by viewModel.isPcapCaptureEnabled.collectAsState()
    val backendIp by viewModel.backendIp.collectAsState()

    var showIpDialog by remember { mutableStateOf(false) }
    var tempIp by remember { mutableStateOf("") }
    val ipv4Regex = Regex("""^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.){3}(25[0-5]|(2[0-4]|1\d|[1-9]|)\d)$""")
    val isIpValid = ipv4Regex.matches(tempIp.trim())

    SettingsSubPage(title = "Advanced", subtitle = "PCAP, exports, and desktop sync", navController = navController) {
        SettingsCard {
            SettingsSwitch(
                icon = Icons.Filled.Policy,
                title = "PCAP Traffic Capture",
                subtitle = "Record raw packets to .pcap file for Wireshark analysis",
                tint = PrimaryBlue,
                checked = isPcapCaptureEnabled,
                onCheckedChange = { viewModel.setPcapCaptureEnabled(it) }
            )

            AnimatedVisibility(visible = isPcapCaptureEnabled) {
                Column {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LocalGKColors.current.accentOrange.copy(alpha = 0.07f))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = LocalGKColors.current.accentOrange, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "⚠️ PCAP files contain raw network data including passwords.\nMax 50 MB per file · 3 files max · Auto-deleted after 24 hours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalGKColors.current.accentOrange
                        )
                    }
                }
            }

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
            SettingsDivider()
            SettingsItem(
                icon = Icons.Filled.Computer,
                title = "Backend IP Address",
                subtitle = backendIp.ifEmpty { "Not configured — tap to set" },
                tint = LocalGKColors.current.primary,
                onClick = { tempIp = backendIp; showIpDialog = true }
            )
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
                        "Enter the IPv4 address of your PC running GateKeeper Desktop (e.g. 192.168.1.100).",
                        color = LocalGKColors.current.textSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempIp,
                        onValueChange = { tempIp = it },
                        label = { Text("IP Address", color = LocalGKColors.current.textTertiary) },
                        singleLine = true,
                        isError = tempIp.isNotEmpty() && !isIpValid,
                        supportingText = {
                            if (tempIp.isNotEmpty() && !isIpValid)
                                Text("Enter a valid IPv4 address (e.g. 192.168.1.100)", color = LocalGKColors.current.accentRed)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (tempIp.isNotEmpty() && !isIpValid) LocalGKColors.current.accentRed else LocalGKColors.current.primary,
                            unfocusedBorderColor = if (tempIp.isNotEmpty() && !isIpValid) LocalGKColors.current.accentRed else LocalGKColors.current.border,
                            focusedTextColor = LocalGKColors.current.textPrimary,
                            unfocusedTextColor = LocalGKColors.current.textPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.setBackendIp(tempIp.trim()); showIpDialog = false },
                    enabled = isIpValid
                ) {
                    Text("Save", color = if (isIpValid) LocalGKColors.current.primary else LocalGKColors.current.textTertiary, fontWeight = FontWeight.Bold)
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

// ─── Sub-page: About ─────────────────────────────────────────────────────────

@Composable
fun SettingsAboutScreen(navController: NavController) {
    SettingsSubPage(title = "About & Features", subtitle = "GateKeeper Mobile v1.0.0", navController = navController) {
        SettingsCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("App Version", style = MaterialTheme.typography.labelMedium, color = LocalGKColors.current.textTertiary)
                Text("GateKeeper Mobile v1.0.0", style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Final Year Project — Mobile Network Security Suite", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
            }
        }

        Spacer(Modifier.height(16.dp))

        val sections = listOf(
            "Network Protection" to listOf(
                "VPN Split-Tunnel (per-app firewall)",
                "Screen-Off blocking — selected apps lose internet access",
                "DNS-over-HTTPS leak prevention",
                "Firewall bypass detection (hardcoded IP)"
            ),
            "Privacy" to listOf(
                "DNS Exfiltration detection (Shannon entropy)",
                "Background camera & microphone alerts",
                "Global camera block (Device Admin)",
                "Real-time sensor access logging"
            ),
            "Threat Detection" to listOf(
                "IMSI Catcher / 2G downgrade detection",
                "Evil Twin Wi-Fi AP detection",
                "Rogue SSL Certificate Auditor",
                "Threat Intelligence feed import"
            ),
            "Forensics" to listOf(
                "PCAP packet capture (Wireshark-compatible)",
                "Connection log export (CSV)",
                "Firewall rules export (JSON)",
                "GateKeeper Desktop integration"
            )
        )

        sections.forEach { (sectionTitle, items) ->
            Text(
                sectionTitle.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = LocalGKColors.current.primary,
                letterSpacing = androidx.compose.ui.unit.TextUnit(0.8f, androidx.compose.ui.unit.TextUnitType.Sp),
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            SettingsCard {
                items.forEachIndexed { i, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                .background(LocalGKColors.current.primary)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(item, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                    }
                    if (i < items.lastIndex) SettingsDivider()
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Shared sub-page scaffold ─────────────────────────────────────────────────

@Composable
fun SettingsSubPage(
    title: String,
    subtitle: String,
    navController: NavController,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalGKColors.current.background)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(LocalGKColors.current.textTertiary.copy(alpha = 0.06f), LocalGKColors.current.background)))
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.displaySmall, color = LocalGKColors.current.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp), content = content)
        Spacer(Modifier.height(80.dp))
    }
}
