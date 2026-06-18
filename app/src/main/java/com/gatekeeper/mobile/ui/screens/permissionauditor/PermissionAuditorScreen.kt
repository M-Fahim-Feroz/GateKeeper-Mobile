package com.gatekeeper.mobile.ui.screens.permissionauditor

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.data.db.entity.SensorLog
import com.gatekeeper.mobile.domain.model.AppPermissionInfo
import com.gatekeeper.mobile.ui.components.GKInfoButton
import com.gatekeeper.mobile.ui.components.GKInfoDialog
import com.gatekeeper.mobile.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PermissionAuditorScreen(
    navController: NavController? = null,
    viewModel: PermissionAuditorViewModel = hiltViewModel()
) {
    val results by viewModel.scannedApps.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scannedCount by viewModel.scannedCount.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val sensorLogs by viewModel.sensorLogs.collectAsState()

    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val tabs = listOf("App Permissions", "Hardware Access")

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF05070A))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LocalGKColors.current.primary,
                    modifier = Modifier.size(24.dp).clickable { navController?.popBackStack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Privacy Dashboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LocalGKColors.current.primary
                )
            }
            GKInfoButton(color = LocalGKColors.current.primary) { showInfoDialog = true }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = LocalGKColors.current.primary,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(2.dp)
                            .background(LocalGKColors.current.primary)
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTab == index) LocalGKColors.current.primary else LocalGKColors.current.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> AppPermissionsTab(results, isScanning, scannedCount, totalCount) { viewModel.scanPermissions() }
                1 -> HardwareAccessTab(
                    sensorLogs = sensorLogs,
                    hasUsageStats = viewModel.hasUsageStatsPermission(),
                    onRefresh = { viewModel.refreshSensorData() },
                    onExport = { viewModel.exportLogs(context) }
                )
            }
        }
    }

    if (showInfoDialog) {
        GKInfoDialog(
            title = "Privacy Dashboard",
            body = "Privacy Dashboard monitors which apps have access to your personal data and when they use it.\n\n• App Permissions: Scans all installed apps and calculates a Risk Score based on how many dangerous permissions they request (like Location, Contacts, Camera).\n• Hardware Access: Shows a live timeline of exactly when apps use your microphone, camera, or location, even in the background.",
            accentColor = LocalGKColors.current.primary,
            onDismiss = { showInfoDialog = false }
        )
    }
}

// ─── TAB 1: App Permission Audit ─────────────────────────────────────────────

@Composable
fun AppPermissionsTab(
    results: List<AppPermissionInfo>,
    isScanning: Boolean,
    scannedCount: Int,
    totalCount: Int,
    onScan: () -> Unit
) {
    // Two sub-tabs: User Apps | System Apps
    var subTab by remember { mutableIntStateOf(0) }

    // Show ALL apps (no risk-tier filter) — sorted highest risk first
    val allFiltered = if (subTab == 0) results.filter { !it.isSystemApp } else results.filter { it.isSystemApp }
    val sortedApps = allFiltered.sortedByDescending { it.riskScore }
    val attentionCount = sortedApps.count { it.riskTier == "CRITICAL" || it.riskTier == "HIGH" }

    LaunchedEffect(Unit) {
        if (results.isEmpty() && !isScanning) onScan()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("App Privacy Scan", style = MaterialTheme.typography.titleLarge, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.Bold)
                    Text(
                        if (isScanning) "Scanning… ($scannedCount / $totalCount)"
                        else "${sortedApps.size} apps • $attentionCount need attention",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalGKColors.current.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (!isScanning) {
                    IconButton(onClick = onScan) {
                        Icon(Icons.Filled.Refresh, null, tint = LocalGKColors.current.primary)
                    }
                }
            }
        }

        // Sub-tab row: User Apps | System Apps
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("User Apps", "System Apps").forEachIndexed { idx, label ->
                    val count = if (idx == 0) results.count { !it.isSystemApp } else results.count { it.isSystemApp }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (subTab == idx) LocalGKColors.current.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (subTab == idx) LocalGKColors.current.primary.copy(alpha = 0.4f) else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { subTab = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (subTab == idx) FontWeight.Bold else FontWeight.Normal,
                                color = if (subTab == idx) LocalGKColors.current.primary else LocalGKColors.current.textSecondary
                            )
                            if (count > 0) {
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(LocalGKColors.current.primary.copy(alpha = if (subTab == idx) 0.2f else 0.08f))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text("$count", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.primary, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isScanning) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = LocalGKColors.current.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("Scanning… $scannedCount / $totalCount", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                    }
                }
            }
        } else if (sortedApps.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No apps found. Tap refresh to scan.", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary, textAlign = TextAlign.Center)
                }
            }
        } else {
            // Group by risk tier for visual separation, but include ALL tiers
            val critical = sortedApps.filter { it.riskTier == "CRITICAL" }
            val high = sortedApps.filter { it.riskTier == "HIGH" }
            val medium = sortedApps.filter { it.riskTier == "MEDIUM" }
            val low = sortedApps.filter { it.riskTier == "LOW" || it.riskTier.isBlank() }

            // Critical
            if (critical.isNotEmpty()) {
                item {
                    RiskSectionHeader("Critical Risk", critical.size, LocalGKColors.current.accentRed)
                }
                item {
                    AppCard(critical)
                }
            }
            // High
            if (high.isNotEmpty()) {
                item {
                    RiskSectionHeader("High Risk", high.size, LocalGKColors.current.accentOrange)
                }
                item {
                    AppCard(high)
                }
            }
            // Medium
            if (medium.isNotEmpty()) {
                item {
                    RiskSectionHeader("Medium Risk", medium.size, LocalGKColors.current.accentYellow)
                }
                item {
                    AppCard(medium)
                }
            }
            // Low
            if (low.isNotEmpty()) {
                item {
                    RiskSectionHeader("Low / No Risk", low.size, LocalGKColors.current.accentGreen)
                }
                item {
                    AppCard(low)
                }
            }
        }
    }
}

@Composable
private fun RiskSectionHeader(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
        }
        Text("$count apps", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary)
    }
}

@Composable
private fun AppCard(apps: List<AppPermissionInfo>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
    ) {
        apps.forEachIndexed { index, app ->
            AuditorAppItem(app)
            if (index < apps.size - 1) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            }
        }
    }
}

@Composable
fun AuditorAppItem(app: AppPermissionInfo) {
    val context = LocalContext.current
    val appName = try {
        context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(app.packageName, 0)).toString()
    } catch (e: Exception) { app.packageName }

    val riskColor = when (app.riskTier) {
        "CRITICAL" -> LocalGKColors.current.accentRed
        "HIGH"     -> LocalGKColors.current.accentOrange
        "MEDIUM"   -> LocalGKColors.current.accentYellow
        else       -> LocalGKColors.current.accentGreen
    }
    val riskLabel = when (app.riskTier) {
        "CRITICAL" -> "Critical"
        "HIGH"     -> "High"
        "MEDIUM"   -> "Medium"
        else       -> "Low"
    }

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            // App Icon
            val appIcon = remember(app.packageName) {
                try { context.packageManager.getApplicationIcon(app.packageName) } catch (e: Exception) { null }
            }
            if (appIcon != null) {
                coil.compose.AsyncImage(
                    model = appIcon, contentDescription = appName,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Android, null, tint = LocalGKColors.current.textSecondary) }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(
                        appName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = LocalGKColors.current.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(riskColor.copy(alpha = 0.12f))
                            .border(1.dp, riskColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(riskLabel, style = MaterialTheme.typography.labelSmall, color = riskColor, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                // Permission pills (top 4)
                val pills = mutableListOf<Pair<String, ImageVector>>()
                if (app.detailedPermissions.any { it.permissionName.contains("LOCATION") && it.effectiveStatus == com.gatekeeper.mobile.domain.model.EffectivePermissionStatus.GRANTED }) pills.add("Location" to Icons.Filled.LocationOn)
                if (app.detailedPermissions.any { it.permissionName.contains("CAMERA") && it.effectiveStatus == com.gatekeeper.mobile.domain.model.EffectivePermissionStatus.GRANTED }) pills.add("Camera" to Icons.Filled.PhotoCamera)
                if (app.detailedPermissions.any { (it.permissionName.contains("RECORD_AUDIO") || it.permissionName.contains("MICROPHONE")) && it.effectiveStatus == com.gatekeeper.mobile.domain.model.EffectivePermissionStatus.GRANTED }) pills.add("Mic" to Icons.Filled.Mic)
                if (app.detailedPermissions.any { it.permissionName.contains("CONTACTS") && it.effectiveStatus == com.gatekeeper.mobile.domain.model.EffectivePermissionStatus.GRANTED }) pills.add("Contacts" to Icons.Filled.Contacts)
                if (app.detailedPermissions.any { (it.permissionName.contains("STORAGE") || it.permissionName.contains("MANAGE_DOCUMENTS")) && it.effectiveStatus == com.gatekeeper.mobile.domain.model.EffectivePermissionStatus.GRANTED }) pills.add("Storage" to Icons.Filled.Folder)
                if (app.detailedPermissions.any { it.permissionName.contains("SMS") && it.effectiveStatus == com.gatekeeper.mobile.domain.model.EffectivePermissionStatus.GRANTED }) pills.add("SMS" to Icons.Filled.Sms)

                if (pills.isEmpty() && app.riskTier == "LOW") {
                    Text("No sensitive permissions granted", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary)
                } else {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        pills.take(4).forEach { (name, icon) ->
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.05f)).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(6.dp)).padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, null, tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(11.dp))
                                Spacer(Modifier.width(3.dp))
                                Text(name, style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textSecondary, fontSize = 10.sp)
                            }
                        }
                        if (pills.size > 4) {
                            Text("+${pills.size - 4} more", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary, modifier = Modifier.align(Alignment.CenterVertically))
                        }
                    }
                }
            }
        }

        // Expanded details
        AnimatedVisibility(visible = expanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(Modifier.height(12.dp))

                // Open Settings button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LocalGKColors.current.primary.copy(alpha = 0.08f))
                        .border(1.dp, LocalGKColors.current.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .clickable {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", app.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Manage App Permissions", style = MaterialTheme.typography.labelMedium, color = LocalGKColors.current.primary, fontWeight = FontWeight.Bold)
                    Icon(Icons.Filled.OpenInNew, null, tint = LocalGKColors.current.primary, modifier = Modifier.size(14.dp))
                }

                Spacer(Modifier.height(12.dp))

                val dangerousGranted = app.detailedPermissions.filter { it.isDangerous }
                    .sortedWith(compareByDescending<com.gatekeeper.mobile.domain.model.DetailedPermission> {
                        it.effectiveStatus == com.gatekeeper.mobile.domain.model.EffectivePermissionStatus.GRANTED
                    }.thenBy { it.permissionName })

                if (dangerousGranted.isNotEmpty()) {
                    Text("Permissions (${dangerousGranted.size})", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    dangerousGranted.forEach { dp ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                dp.permissionName.substringAfterLast("."),
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalGKColors.current.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            val statusColor = when (dp.effectiveStatus) {
                                com.gatekeeper.mobile.domain.model.EffectivePermissionStatus.GRANTED -> Color(0xFF37DF66)
                                com.gatekeeper.mobile.domain.model.EffectivePermissionStatus.DENIED -> Color(0xFFF44336)
                                com.gatekeeper.mobile.domain.model.EffectivePermissionStatus.FOREGROUND_ONLY -> Color(0xFFFFC107)
                                else -> Color.Gray
                            }
                            Text(dp.effectiveStatus.name, style = MaterialTheme.typography.labelSmall, color = statusColor)
                        }
                    }
                } else {
                    Text("No dangerous permissions declared.", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textTertiary)
                }
            }
        }

        // Tap hint
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (expanded) "Collapse" else "Details",
                style = MaterialTheme.typography.labelSmall,
                color = LocalGKColors.current.primary
            )
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                null,
                tint = LocalGKColors.current.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── TAB 2: Hardware Access Control ─────────────────────────────────────────

@Composable
fun HardwareAccessTab(
    sensorLogs: List<SensorLog>,
    hasUsageStats: Boolean = false,
    onRefresh: () -> Unit = {},
    onExport: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFormatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    // Sort all logs newest-first — no filtering, no cap
    val allLogs = remember(sensorLogs) { sensorLogs.sortedByDescending { it.startedAt } }
    val totalEvents = allLogs.size

    // Session grouping:
    // A "session" = one package+sensorType combination
    // The session's timestamp = the most recent log in that group
    // Sessions sorted by their most recent log (newest session first)
    data class SessionKey(val packageName: String, val sensorType: String)
    val sessions = remember(allLogs) {
        allLogs.groupBy { SessionKey(it.packageName, it.sensorType) }
            .entries
            .sortedByDescending { (_, logs) -> logs.maxOf { it.startedAt } }
    }

    // Which sessions are expanded
    val expandedSessions = remember { mutableStateMapOf<String, Boolean>() }

    // Stats
    val locationCount = allLogs.count { it.sensorType == "LOCATION" }
    val cameraCount = allLogs.count { it.sensorType == "CAMERA" }
    val micCount = allLogs.count { it.sensorType == "MICROPHONE" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Permission warning ──────────────────────────────────────────
        if (!hasUsageStats) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFB300).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Usage Access Required", style = MaterialTheme.typography.titleSmall, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.Bold)
                            Text("Grant usage access to see real-time sensor activity. Tap to open settings.", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                        }
                        Icon(Icons.Filled.OpenInNew, null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                    }
                    
                    HorizontalDivider(color = Color(0xFFFFB300).copy(alpha = 0.1f))
                    
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("To grant Usage Access to Gatekeeper, please follow these steps:", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        
                        Text("1. Allow Restricted Settings", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("• Open Settings on your phone.\n• Go to Apps → See all apps.\n• Scroll through the full app list and select Gatekeeper.\n   (Important: Make sure you open Gatekeeper from this complete apps list. If you open it from Recent Apps or the home screen, the required option may not appear).\n• On the App Info screen, tap the three-dot menu (top-right corner).\n• Select 'Allow restricted settings'.\n• Verify your identity using your PIN, password, or biometric authentication.", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Text("2. Grant Usage Access", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("• Return to Settings.\n• Navigate to Apps → Special app access → Usage access.\n• Select Gatekeeper.\n• Turn on 'Permit usage access'.", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                        
                        Spacer(Modifier.height(16.dp))
                        Text("Once completed, Gatekeeper will have the required permission and function correctly.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Header row ─────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Hardware Access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textPrimary)
                    Text("Last 24 hours • $totalEvents events", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                }
                Row {
                    IconButton(onClick = { onExport?.invoke() }) {
                        Icon(Icons.Filled.Download, null, tint = LocalGKColors.current.primary)
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, null, tint = LocalGKColors.current.primary)
                    }
                }
            }
        }

        // ── Stats row ──────────────────────────────────────────────────
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SensorStatCard("Location", locationCount, Color(0xFF00D4FF), Icons.Filled.LocationOn, Modifier.weight(1f))
                SensorStatCard("Camera", cameraCount, Color(0xFF37DF66), Icons.Filled.Videocam, Modifier.weight(1f))
                SensorStatCard("Mic", micCount, Color(0xFFFF9800), Icons.Filled.Mic, Modifier.weight(1f))
            }
        }

        // ── Empty state ────────────────────────────────────────────────
        if (allLogs.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.SensorsOff, null, tint = LocalGKColors.current.textTertiary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No sensor activity in the last 24 hours.", style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textSecondary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Text("Tap refresh to check again.", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textTertiary, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            // ── Session-grouped list ───────────────────────────────────
            item {
                Text(
                    "${sessions.size} apps used sensors • tap to expand",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalGKColors.current.textTertiary
                )
            }

            sessions.forEach { (key, logsInSession) ->
                val sessionKey = "${key.packageName}::${key.sensorType}"
                // Pre-compute non-composable values outside item{}
                val lastAccessTime = logsInSession.maxOf { it.startedAt }
                val sensorColorValue = when (key.sensorType) {
                    "CAMERA" -> Color(0xFF37DF66)
                    "MICROPHONE" -> Color(0xFFFF9800)
                    "LOCATION" -> Color(0xFF00D4FF)
                    else -> Color(0xFF859398) // neutral grey — no composable call
                }
                val sensorIcon = when (key.sensorType) {
                    "CAMERA" -> Icons.Filled.Videocam
                    "MICROPHONE" -> Icons.Filled.Mic
                    "LOCATION" -> Icons.Filled.LocationOn
                    else -> Icons.Filled.Sensors
                }
                val hasAlert = logsInSession.any { it.isBackground || !it.isAllowed }

                item(key = sessionKey) {
                    // Composable-safe values resolved inside item{}
                    val isExpanded = expandedSessions[sessionKey] == true
                    val appName = remember(key.packageName) {
                        try { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(key.packageName, 0)).toString() }
                        catch (e: Exception) { key.packageName }
                    }
                    val sensorColor = sensorColorValue // already a plain Color, safe to use

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (hasAlert) LocalGKColors.current.accentRed.copy(alpha = 0.04f)
                                else Color.White.copy(alpha = 0.03f)
                            )
                            .border(
                                1.dp,
                                if (hasAlert) LocalGKColors.current.accentRed.copy(alpha = 0.2f)
                                else Color.White.copy(alpha = 0.07f),
                                RoundedCornerShape(12.dp)
                            )
                    ) {
                        // ── Session header (always visible) ────────────
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedSessions[sessionKey] = !isExpanded
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // App icon
                            val appIcon = remember(key.packageName) {
                                try { context.packageManager.getApplicationIcon(key.packageName) }
                                catch (e: Exception) { null }
                            }
                            if (appIcon != null) {
                                coil.compose.AsyncImage(
                                    model = appIcon, contentDescription = appName,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(sensorColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) { Icon(sensorIcon, null, tint = sensorColor, modifier = Modifier.size(20.dp)) }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = LocalGKColors.current.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                    if (hasAlert) {
                                        Spacer(Modifier.width(6.dp))
                                        Icon(Icons.Filled.Warning, null, tint = LocalGKColors.current.accentRed, modifier = Modifier.size(13.dp))
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                                    Icon(sensorIcon, null, tint = sensorColor, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(key.sensorType.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = sensorColor)
                                    Spacer(Modifier.width(8.dp))
                                    Text("${logsInSession.size} events", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Last: ${dateFormatter.format(Date(lastAccessTime))}", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary)
                                }
                            }

                            // Expand/Manage row
                            Column(horizontalAlignment = Alignment.End) {
                                Icon(
                                    if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    null,
                                    tint = LocalGKColors.current.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // ── Expanded: full event timeline ──────────────
                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

                                // "Open in Settings" shortcut
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            try {
                                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                    data = Uri.fromParts("package", key.packageName, null)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {}
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Manage ${appName} permissions", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.primary)
                                    Icon(Icons.Filled.OpenInNew, null, tint = LocalGKColors.current.primary, modifier = Modifier.size(13.dp))
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.04f))

                                // Column header
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Time", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary, modifier = Modifier.width(90.dp))
                                    Text("Context", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary, modifier = Modifier.width(80.dp))
                                    Text("Status", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary)
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.04f))

                                // Every individual log entry — no truncation
                                logsInSession.forEachIndexed { idx, log ->
                                    val isAlert = log.isBackground || !log.isAllowed
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (isAlert) LocalGKColors.current.accentRed.copy(alpha = 0.04f) else Color.Transparent)
                                            .padding(horizontal = 14.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            timeFormatter.format(Date(log.startedAt)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LocalGKColors.current.textSecondary,
                                            modifier = Modifier.width(90.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(80.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (log.isBackground) LocalGKColors.current.accentOrange.copy(alpha = 0.12f)
                                                    else Color.White.copy(alpha = 0.05f)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                if (log.isBackground) "Background" else "Foreground",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = if (log.isBackground) LocalGKColors.current.accentOrange else LocalGKColors.current.textSecondary
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (log.isAllowed) LocalGKColors.current.accentGreen.copy(alpha = 0.12f)
                                                    else LocalGKColors.current.accentRed.copy(alpha = 0.12f)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                if (log.isAllowed) "Allowed" else "Blocked",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = if (log.isAllowed) LocalGKColors.current.accentGreen else LocalGKColors.current.accentRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (idx < logsInSession.size - 1) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.03f))
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorStatCard(label: String, count: Int, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.07f))
            .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(4.dp))
            Text("$count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun HardwareLegendItem(label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textSecondary)
    }
}

@Composable
fun SensorTimelineEvent(
    appName: String, stateLabel: String, isAlert: Boolean,
    iconName: String, detailText: String, timeText: String,
    iconColor: Color, isLast: Boolean = false
) {
    val icon = when (iconName) {
        "location_on" -> Icons.Filled.LocationOn
        "videocam" -> Icons.Filled.Videocam
        "mic" -> Icons.Filled.Mic
        else -> Icons.Filled.Sensors
    }
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(modifier = Modifier.width(32.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            if (!isLast) Box(modifier = Modifier.width(2.dp).fillMaxHeight().padding(top = 24.dp).background(Color.White.copy(alpha = 0.05f)))
            Box(modifier = Modifier.padding(top = 16.dp).size(16.dp).clip(CircleShape).background(if (isAlert) LocalGKColors.current.accentRed else Color(0xFF859398)).border(4.dp, Color(0xFF05070A), CircleShape))
        }
        Column(
            modifier = Modifier.weight(1f).padding(vertical = 12.dp)
                .border(bottom = if (!isLast) 1.dp else 0.dp, color = Color.White.copy(alpha = 0.05f))
                .padding(bottom = 12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(appName, style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textPrimary)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                .background(if (isAlert) LocalGKColors.current.accentRed.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                .border(1.dp, if (isAlert) LocalGKColors.current.accentRed.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(stateLabel.uppercase(), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isAlert) LocalGKColors.current.accentRed else LocalGKColors.current.textSecondary)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(detailText, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                    }
                }
                Text(timeText, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textTertiary)
            }
        }
    }
}

// Extension for bottom-border-only modifier
fun Modifier.border(bottom: androidx.compose.ui.unit.Dp, color: Color): Modifier = this.drawBehind {
    val strokeWidth = bottom.toPx()
    val y = size.height - strokeWidth / 2
    drawLine(color = color, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = strokeWidth)
}
