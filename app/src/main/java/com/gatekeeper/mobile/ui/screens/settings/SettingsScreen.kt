package com.gatekeeper.mobile.ui.screens.settings

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.theme.*

@Composable
fun SettingsScreen(
    navController: NavController? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val backendIp by viewModel.backendIp.collectAsState("")
    val isPcapCaptureEnabled by viewModel.isPcapCaptureEnabled.collectAsState()

    var showIpDialog by remember { mutableStateOf(false) }
    var tempIp by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
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
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(TextSecondary, TextTertiary).map { it.copy(alpha = 0.2f) })),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Settings, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Settings", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                    Text("Configure your security suite", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            // Integration Section
            Text("GateKeeper Desktop Integration", style = MaterialTheme.typography.titleMedium, color = PrimaryCyan, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
            
            SettingsCard {
                SettingsItem(
                    icon = Icons.Filled.Computer,
                    title = "Backend IP Address",
                    subtitle = backendIp.ifEmpty { "Not configured" },
                    onClick = { tempIp = backendIp; showIpDialog = true }
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Advanced VPN Settings
            Text("Advanced Options", style = MaterialTheme.typography.titleMedium, color = PrimaryCyan, modifier = Modifier.padding(bottom = 8.dp, start = 4.dp))
            
            SettingsCard {
                SettingsSwitch(
                    icon = Icons.Filled.Policy,
                    title = "PCAP Traffic Capture",
                    subtitle = "Capture raw packets for analysis",
                    checked = isPcapCaptureEnabled,
                    onCheckedChange = { viewModel.setPcapCaptureEnabled(it) }
                )
            }
            
            Spacer(Modifier.height(30.dp))
            
            // About
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("GateKeeper Mobile v1.0.0", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Text("Core security engine powered by Android VpnService", style = MaterialTheme.typography.bodySmall, color = TextTertiary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showIpDialog) {
        AlertDialog(
            onDismissRequest = { showIpDialog = false },
            containerColor = DarkSurfaceVariant,
            title = { Text("Configure Backend IP", color = TextPrimary) },
            text = {
                Column {
                    Text("Enter the local IPv4 address of your desktop running GateKeeper core (e.g., 192.168.1.100).", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
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
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val color = if (isDestructive) AccentRed else PrimaryCyan
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (isDestructive) AccentRed else TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = PrimaryCyan, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = PrimaryCyan,
                checkedTrackColor = PrimaryCyan.copy(alpha = 0.3f),
                uncheckedThumbColor = TextTertiary,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}
