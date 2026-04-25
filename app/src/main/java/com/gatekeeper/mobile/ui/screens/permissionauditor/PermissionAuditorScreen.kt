package com.gatekeeper.mobile.ui.screens.permissionauditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.navigation.NavController
import com.gatekeeper.mobile.domain.model.AppPermissionInfo
import com.gatekeeper.mobile.ui.components.*
import com.gatekeeper.mobile.ui.theme.*

@Composable
fun PermissionAuditorScreen(
    navController: NavController? = null,
    viewModel: PermissionAuditorViewModel = hiltViewModel()
) {
    val results by viewModel.scannedApps.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AccentYellow.copy(alpha = 0.08f), DarkBackground)))
                .padding(horizontal = 20.dp, vertical = 20.dp)
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
                        .background(Brush.linearGradient(listOf(AccentYellow, AccentOrange).map { it.copy(alpha = 0.2f) })),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.VerifiedUser, null, tint = AccentYellow, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Permission Auditor", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                    Text("Scan apps for privacy risks", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.scanPermissions() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentYellow,
                    contentColor = DarkBackground,
                    disabledContainerColor = DarkSurfaceVariant
                )
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = TextTertiary)
                    Spacer(Modifier.width(12.dp))
                    Text("Analyzing installed apps...", color = TextTertiary, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.Search, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start Full Audit", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (results.isEmpty() && !isScanning) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.SecurityUpdateWarning, null, tint = TextTertiary, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Run an audit to find risky apps", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val highRisk = results.filter { it.riskLevel.equals("high", ignoreCase = true) }.sortedByDescending { it.riskScore }
                val mediumRisk = results.filter { it.riskLevel.equals("medium", ignoreCase = true) }.sortedByDescending { it.riskScore }
                
                if (highRisk.isNotEmpty()) {
                    item { SectionHeader(title = "High Risk Apps (${highRisk.size})") }
                    items(highRisk, key = { it.packageName }) { app -> AuditorAppItem(app) }
                }
                
                if (mediumRisk.isNotEmpty()) {
                    item { SectionHeader(title = "Medium Risk Apps (${mediumRisk.size})") }
                    items(mediumRisk, key = { it.packageName }) { app -> AuditorAppItem(app) }
                }
                
                if (highRisk.isEmpty() && mediumRisk.isEmpty() && results.isNotEmpty()) {
                    item { 
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No risky apps found. Looking good!", color = AccentGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuditorAppItem(appInfo: AppPermissionInfo) {
    val isHighRisk = appInfo.riskLevel.equals("high", ignoreCase = true)
    val accentColor = if (isHighRisk) AccentRed else AccentOrange
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val appIcon = remember(appInfo.packageName) {
        try { context.packageManager.getApplicationIcon(appInfo.packageName) } catch (e: Exception) { null }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (appIcon != null) {
                    coil.compose.AsyncImage(model = appIcon, contentDescription = appInfo.appName, modifier = Modifier.size(28.dp))
                } else {
                    Icon(Icons.Filled.Android, null, tint = accentColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(appInfo.appName, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(appInfo.packageName, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            
            Box(
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(accentColor.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    if (isHighRisk) "HIGH RISK" else "MODERATE",
                    style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(Modifier.height(12.dp))
        Divider(color = GlassBorder)
        Spacer(Modifier.height(12.dp))
        
        Text("Sensitive Permissions:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            appInfo.dangerousPermissions.forEach { permission ->
                val permName = permission.substringAfterLast(".")
                val isVerySensitive = permName in listOf("CAMERA", "RECORD_AUDIO", "ACCESS_FINE_LOCATION", "READ_CONTACTS", "READ_SMS")
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isVerySensitive) AccentRed.copy(alpha = 0.1f) else DarkSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        permName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isVerySensitive) AccentRed else TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", appInfo.packageName, null)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().height(42.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isHighRisk) AccentRed.copy(alpha = 0.15f) else PrimaryCyan.copy(alpha = 0.15f),
                contentColor = if (isHighRisk) AccentRed else PrimaryCyan
            )
        ) {
            Icon(Icons.Filled.Settings, "Manage", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Manage Permissions in OS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        }
    }
}
