package com.gatekeeper.mobile.ui.screens.permissionauditor

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
 import androidx.compose.foundation.border
 import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.data.db.entity.SensorLog
import com.gatekeeper.mobile.domain.model.AppPermissionInfo
import com.gatekeeper.mobile.ui.components.*
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

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("App Permissions", "Hardware Access")

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
                    Text("Privacy Guard", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                    Text("Permissions & Hardware Access Control", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkSurface,
            contentColor = PrimaryCyan,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(2.dp)
                            .background(PrimaryCyan)
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
                            color = if (selectedTab == index) PrimaryCyan else TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> AppPermissionsTab(results, isScanning, scannedCount, totalCount) { viewModel.scanPermissions() }
            1 -> HardwareAccessTab(sensorLogs)
        }
    }
}

// ─── TAB 1: App Permission Audit ───────────────────────────────────────────

@Composable
fun AppPermissionsTab(
    results: List<AppPermissionInfo>,
    isScanning: Boolean,
    scannedCount: Int,
    totalCount: Int,
    onScan: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Scan button
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            val isComplete = !isScanning && results.isNotEmpty()
            val buttonBg = if (isComplete) Color.Transparent else AccentYellow
            val buttonContent = if (isComplete) AccentGreen else DarkBackground
            
            Button(
                onClick = onScan,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !isScanning,
                border = if (isComplete) androidx.compose.foundation.BorderStroke(1.dp, AccentGreen) else null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBg,
                    contentColor = buttonContent,
                    disabledContainerColor = DarkSurfaceVariant
                )
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = TextTertiary)
                    Spacer(Modifier.width(12.dp))
                    Text("Scanning apps… ($scannedCount / $totalCount)", color = TextTertiary, fontWeight = FontWeight.Bold)
                } else if (isComplete) {
                    Text("Audit complete · Tap to re-scan", fontWeight = FontWeight.Bold)
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
                if (results.isNotEmpty()) {
                    item {
                        Text("🔴 Red = real-time surveillance risk  ·  ⬜ Gray = data access risk", style = MaterialTheme.typography.labelSmall, color = TextTertiary, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }

                val critical = results.filter { it.riskTier == "CRITICAL" }
                val high     = results.filter { it.riskTier == "HIGH" }
                val medium   = results.filter { it.riskTier == "MEDIUM" }
                val low      = results.filter { it.riskTier == "LOW" }

                if (critical.isNotEmpty()) {
                    item { SectionHeader(title = "Critical Risk Apps (${critical.size})") }
                    items(critical, key = { it.packageName }) { app -> AuditorAppItem(app) }
                }

                if (high.isNotEmpty()) {
                    item { SectionHeader(title = "High Risk Apps (${high.size})") }
                    items(high, key = { it.packageName }) { app -> AuditorAppItem(app) }
                }

                if (medium.isNotEmpty()) {
                    item { SectionHeader(title = "Medium Risk Apps (${medium.size})") }
                    items(medium, key = { it.packageName }) { app -> AuditorAppItem(app) }
                }

                if (low.isNotEmpty()) {
                    item {
                        var expanded by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Show ${low.size} low-risk apps", style = MaterialTheme.typography.bodyMedium, color = TextTertiary, modifier = Modifier.weight(1f))
                            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = TextTertiary)
                        }
                        if (expanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                low.forEach { app -> AuditorAppItem(app) }
                            }
                        }
                    }
                }

                if (results.isNotEmpty() && critical.isEmpty() && high.isEmpty() && medium.isEmpty()) {
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

// ─── TAB 2: Hardware Access Control ────────────────────────────────────────

data class HardwareSensor(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val permission: String  // Android permission name for settings deeplink
)

private val HARDWARE_SENSORS = listOf(
    HardwareSensor("Camera", "Take photos and record video", Icons.Filled.PhotoCamera, AccentRed, "android.permission.CAMERA"),
    HardwareSensor("Microphone", "Record audio and voice", Icons.Filled.Mic, AccentOrange, "android.permission.RECORD_AUDIO"),
    HardwareSensor("Location", "Track precise GPS position", Icons.Filled.LocationOn, AccentYellow, "android.permission.ACCESS_FINE_LOCATION"),
    HardwareSensor("Contacts", "Read and write contacts", Icons.Filled.Contacts, PrimaryCyan, "android.permission.READ_CONTACTS"),
    HardwareSensor("Storage", "Read and write files", Icons.Filled.Folder, Color(0xFF7C83FD), "android.permission.READ_EXTERNAL_STORAGE")
)

@Composable
fun HardwareAccessTab(sensorLogs: List<SensorLog>) {
    val context = LocalContext.current

    // Group logs by sensor type for analytics
    val logsByType = sensorLogs.groupBy { it.sensorType }
    val recentAccessors = sensorLogs
        .filter { System.currentTimeMillis() - it.startedAt < 24 * 60 * 60 * 1000 }
        .groupBy { it.packageName }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick info banner
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PrimaryCyan.copy(alpha = 0.08f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Info, null, tint = PrimaryCyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Tap any sensor to view apps. The Block / Unblock buttons open Android's permission settings to revoke or grant access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Global System Blocks
        item {
            Spacer(Modifier.height(8.dp))
            Text("Global System Blocks", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            
            // Camera/Mic system toggles (Android 12+) or Privacy Dashboard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .clickable { 
                        try {
                            context.startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS))
                        } catch (e: Exception) {
                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                        .background(AccentRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Block, null, tint = AccentRed, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("System-Wide Sensor Block", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Open Android's global privacy controls to cut off Camera & Mic access for ALL apps instantly.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Icon(Icons.Filled.OpenInNew, null, tint = PrimaryCyan, modifier = Modifier.size(20.dp))
            }
        }
        
        item {
            Spacer(Modifier.height(16.dp))
            Text("App-Specific Access", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        }

        // Hardware sensor cards with access log
        items(HARDWARE_SENSORS) { sensor ->
            HardwareSensorCard(sensor = sensor, logs = logsByType[sensor.name.uppercase()] ?: emptyList())
        }

        // Recent 24h background access timeline
        if (sensorLogs.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Recent Access Timeline (24h)", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
            }

            val recentLogs = sensorLogs
                .filter { System.currentTimeMillis() - it.startedAt < 24 * 60 * 60 * 1000 }
                .sortedByDescending { it.startedAt }

            items(recentLogs, key = { it.id }) { log ->
                SensorLogItem(log)
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun HardwareSensorCard(sensor: HardwareSensor, logs: List<SensorLog>) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    // Group log by app
    val appAccesses = logs.groupBy { it.packageName }
    val backgroundAccesses = logs.filter { it.isBackground }
    val recentCount = logs.filter { System.currentTimeMillis() - it.startedAt < 24 * 60 * 60 * 1000 }.size

    val riskColor = when {
        backgroundAccesses.isNotEmpty() -> AccentRed
        recentCount > 5 -> AccentOrange
        else -> sensor.accentColor
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
    ) {
        // Sensor header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(riskColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(sensor.icon, null, tint = riskColor, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(sensor.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(sensor.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }

            // Stats badges
            Column(horizontalAlignment = Alignment.End) {
                if (recentCount > 0) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(riskColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("$recentCount accesses today", style = MaterialTheme.typography.labelSmall, color = riskColor, fontWeight = FontWeight.Bold)
                    }
                    if (backgroundAccesses.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text("⚠️ BG access", style = MaterialTheme.typography.labelSmall, color = AccentRed)
                    }
                } else {
                    Text("No access today", style = MaterialTheme.typography.bodySmall, color = AccentGreen)
                }
                Spacer(Modifier.height(2.dp))
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null, tint = TextTertiary, modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expanded: per-app list with revoke buttons
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(200)) + fadeIn()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                HorizontalDivider(color = GlassBorder)
                Spacer(Modifier.height(12.dp))

                if (appAccesses.isEmpty()) {
                    Text("No apps accessed ${sensor.name} recently.", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                } else {
                    Text("Apps that accessed ${sensor.name}:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))

                    appAccesses.forEach { (pkg, pkgLogs) ->
                        val appName = try {
                            context.packageManager.getApplicationLabel(
                                context.packageManager.getApplicationInfo(pkg, 0)
                            ).toString()
                        } catch (e: Exception) { pkg }
                        val bgCount = pkgLogs.filter { it.isBackground }.size

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface)
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val appIcon = remember(pkg) {
                                    try { context.packageManager.getApplicationIcon(pkg) } catch (e: Exception) { null }
                                }
                                if (appIcon != null) {
                                    coil.compose.AsyncImage(model = appIcon, contentDescription = appName, modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)))
                                } else {
                                    Icon(Icons.Filled.Android, null, tint = TextTertiary, modifier = Modifier.size(24.dp))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(appName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${pkgLogs.size} access${if (pkgLogs.size > 1) "es" else ""}${if (bgCount > 0) " • $bgCount background ⚠️" else ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (bgCount > 0) AccentRed else TextSecondary
                                    )
                                }
                                // *** KEY FEATURE: Open EXACT permission page for THIS sensor ***
                                // On Android 12+, navigates directly to Camera/Mic/Location
                                // permission toggle for this specific app
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            // Try the direct permission management intent (Android 12+)
                                            val intent = Intent("android.intent.action.MANAGE_APP_PERMISSIONS").apply {
                                                putExtra(Intent.EXTRA_PACKAGE_NAME, pkg)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Fallback: App details settings
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                                data = Uri.fromParts("package", pkg, null)
                                            }
                                            context.startActivity(intent)
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, sensor.accentColor.copy(alpha = 0.5f)),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Filled.Block, null, tint = sensor.accentColor, modifier = Modifier.size(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Block / Unblock", style = MaterialTheme.typography.labelSmall, color = sensor.accentColor)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }

                // "Manage All" button — goes directly to the permission group in Android settings
                Spacer(Modifier.height(4.dp))

                // GateKeeper enforcement explanation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(sensor.accentColor.copy(alpha = 0.06f))
                        .padding(10.dp)
                ) {
                    Icon(Icons.Filled.Info, null, tint = sensor.accentColor.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "GateKeeper monitors when apps access ${sensor.name}. Background access triggers a security alert. " +
                        "Use 'Revoke ${sensor.name}' to open the exact permission page for each app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        // Android 12+ Permission Manager — opens the permission group directly
                        try {
                            val intent = Intent("android.intent.action.MANAGE_PERMISSION_APPS").apply {
                                putExtra("android.intent.extra.PERMISSION_NAME", sensor.permission)
                            }
                            context.startActivity(intent)
                        } catch (e1: Exception) {
                            try {
                                context.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS))
                            } catch (e2: Exception) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, sensor.accentColor.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, null, tint = sensor.accentColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("View All Apps with ${sensor.name} Access", color = sensor.accentColor, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun SensorLogItem(log: SensorLog) {
    val context = LocalContext.current
    val appName = remember(log.packageName) {
        try {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(log.packageName, 0)
            ).toString()
        } catch (e: Exception) { log.appName }
    }
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    val (icon, color) = when (log.sensorType) {
        "CAMERA" -> Icons.Filled.PhotoCamera to AccentRed
        "MICROPHONE" -> Icons.Filled.Mic to AccentOrange
        "LOCATION" -> Icons.Filled.LocationOn to AccentYellow
        else -> Icons.Filled.Sensors to PrimaryCyan
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkCard)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(appName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                if (log.isBackground) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(AccentRed.copy(alpha = 0.15f)).padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text("BG", style = MaterialTheme.typography.labelSmall, color = AccentRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                "${log.sensorType} • ${formatter.format(Date(log.startedAt))}${if (log.durationMs > 0) " (${log.durationMs / 1000}s)" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        // Quick revoke for logged apps
        IconButton(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", log.packageName, null)
                }
                context.startActivity(intent)
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Filled.Settings, "Manage", tint = TextTertiary, modifier = Modifier.size(16.dp))
        }
    }
}

// ─── Reusable ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuditorAppItem(appInfo: AppPermissionInfo) {
    val accentColor = when (appInfo.riskTier) {
        "CRITICAL" -> AccentRed
        "HIGH" -> AccentOrange
        "MEDIUM" -> AccentYellow
        else -> TextTertiary
    }

    val context = LocalContext.current
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
                    appInfo.riskTier,
                    style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = GlassBorder)
        Spacer(Modifier.height(12.dp))

        Text("Sensitive Permissions:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(6.dp))

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            appInfo.dangerousPermissions.forEach { permission ->
                val permName = permission.substringAfterLast(".")
                val isSurveillance = permName in listOf("CAMERA", "RECORD_AUDIO", "ACCESS_FINE_LOCATION", "READ_CONTACTS", "ACCESS_BACKGROUND_LOCATION")

                if (isSurveillance) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentRed)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            permName,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .border(1.dp, TextTertiary, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            permName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
                containerColor = accentColor.copy(alpha = 0.15f),
                contentColor = accentColor
            )
        ) {
            Icon(Icons.Filled.Settings, "Manage", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Manage Permissions in OS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        }
    }
}
