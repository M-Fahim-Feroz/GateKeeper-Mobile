package com.gatekeeper.mobile.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.theme.*

@Composable
fun SettingsLandingScreen(navController: NavController) {
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
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
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
                    Text("Security configuration", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(8.dp))

            SettingsCard {
                SettingsNavRow(
                    icon = Icons.Filled.Shield,
                    title = "Protection",
                    subtitle = "VPN blocking, DNS privacy, bypass detection",
                    tint = PrimaryCyan,
                    onClick = { navController.navigate("settings/protection") }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Lock,
                    title = "Privacy",
                    subtitle = "Sensor alerts, camera block, IMSI & Evil Twin detection",
                    tint = AccentYellow,
                    onClick = { navController.navigate("settings/privacy") }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Build,
                    title = "Advanced",
                    subtitle = "PCAP capture, log export, backend connection",
                    tint = PrimaryBlue,
                    onClick = { navController.navigate("settings/advanced") }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Info,
                    title = "About & Features",
                    subtitle = "Version info, security feature overview",
                    tint = TextTertiary,
                    onClick = { navController.navigate("settings/about") }
                )
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color = PrimaryCyan,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(20.dp))
    }
}
