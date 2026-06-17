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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.navigation.safeNavigate
import com.gatekeeper.mobile.ui.theme.*

@Composable
fun SettingsLandingScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalGKColors.current.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(LocalGKColors.current.textTertiary.copy(alpha = 0.08f), LocalGKColors.current.background)))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
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
                    Text("Security configuration", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))

            // Appearance Section
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleSmall,
                color = LocalGKColors.current.textSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Theme", color = LocalGKColors.current.textPrimary, style = MaterialTheme.typography.bodyMedium)
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(LocalGKColors.current.surfaceVariant)
                    ) {
                        ThemeChip("System", isSelected = themeMode == 0) { viewModel.setThemeMode(0) }
                        ThemeChip("Light", isSelected = themeMode == 1) { viewModel.setThemeMode(1) }
                        ThemeChip("Dark", isSelected = themeMode == 2) { viewModel.setThemeMode(2) }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Modules",
                style = MaterialTheme.typography.titleSmall,
                color = LocalGKColors.current.textSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
            SettingsCard {
                SettingsNavRow(
                    icon = Icons.Filled.Shield,
                    title = "Protection",
                    subtitle = "VPN blocking, DNS privacy, bypass detection",
                    tint = LocalGKColors.current.primary,
                    onClick = { navController.safeNavigate("settings/protection") }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Lock,
                    title = "Privacy",
                    subtitle = "Sensor alerts, camera block, IMSI & Evil Twin detection",
                    tint = LocalGKColors.current.accentYellow,
                    onClick = { navController.safeNavigate("settings/privacy") }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Build,
                    title = "Advanced",
                    subtitle = "PCAP capture, log export, backend connection",
                    tint = PrimaryBlue,
                    onClick = { navController.safeNavigate("settings/advanced") }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Info,
                    title = "About & Features",
                    subtitle = "Version info, security feature overview",
                    tint = LocalGKColors.current.textTertiary,
                    onClick = { navController.safeNavigate("settings/about") }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Filled.Science,
                    title = "Technical Transparency",
                    subtitle = "OS limits & architectural notes (FYP)",
                    tint = LocalGKColors.current.accentOrange,
                    onClick = { navController.safeNavigate("limitations") }
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
    tint: Color = LocalGKColors.current.primary,
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
            Text(title, style = MaterialTheme.typography.titleMedium, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = LocalGKColors.current.textTertiary, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ThemeChip(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (isSelected) LocalGKColors.current.primary else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) LocalGKColors.current.textOnPrimary else LocalGKColors.current.textSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

