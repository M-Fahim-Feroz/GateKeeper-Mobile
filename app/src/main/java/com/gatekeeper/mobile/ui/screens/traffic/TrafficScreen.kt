package com.gatekeeper.mobile.ui.screens.traffic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.gatekeeper.mobile.data.db.entity.ConnectionLog
import com.gatekeeper.mobile.ui.components.*
import com.gatekeeper.mobile.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficScreen(viewModel: TrafficViewModel = hiltViewModel()) {
    val allConnections by viewModel.recentConnections.collectAsState(initial = emptyList())
    val connections = remember(allConnections) { allConnections.take(200) }

    val totalCount by viewModel.totalConnections.collectAsState(initial = 0)
    val timeRange by viewModel.timeRange.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()

    val blockedCount = remember(connections) { connections.count { it.wasBlocked && !it.isSystemEvent } }
    val allowedCount = remember(connections) { connections.count { !it.wasBlocked && !it.isSystemEvent } }

    val bandwidthUsage by viewModel.bandwidthUsage.collectAsState(initial = emptyMap())

    var selectedLog by remember { mutableStateOf<ConnectionLog?>(null) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Connections, 1 = Data Usage
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // ── Header ──────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(AccentGreen.copy(alpha = 0.08f), DarkBackground)))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(GradientSuccess.map { it.copy(alpha = 0.2f) })),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.NetworkCheck, null, tint = AccentGreen, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Traffic Monitor", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                    Text("Network connection log · last $timeRange", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Live stats
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Blocked stat — red
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentRed.copy(alpha = 0.08f))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Block, null, tint = AccentRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("$blockedCount", style = MaterialTheme.typography.titleLarge, color = AccentRed, fontWeight = FontWeight.Bold)
                    Text("Blocked", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
                // Allowed stat — green
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentGreen.copy(alpha = 0.08f))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("$allowedCount", style = MaterialTheme.typography.titleLarge, color = AccentGreen, fontWeight = FontWeight.Bold)
                    Text("Allowed", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
                // Total stat — cyan
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryCyan.copy(alpha = 0.06f))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Storage, null, tint = PrimaryCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("$totalCount", style = MaterialTheme.typography.titleLarge, color = PrimaryCyan, fontWeight = FontWeight.Bold)
                    Text("Total ever", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Time range filter
            GKFilterChips(
                options = listOf("1h", "24h", "7d"),
                selected = timeRange,
                onSelect = { viewModel.setTimeRange(it) }
            )
            Spacer(Modifier.height(16.dp))

            // Mode toggle chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Connections") },
                    leadingIcon = { @Suppress("DEPRECATION") Icon(Icons.Filled.List, null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryCyan.copy(alpha = 0.15f),
                        selectedLabelColor = PrimaryCyan
                    )
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Data Usage") },
                    leadingIcon = { Icon(Icons.Filled.DataUsage, null, modifier = Modifier.size(16.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentOrange.copy(alpha = 0.15f),
                        selectedLabelColor = AccentOrange
                    )
                )
            }
            
            if (selectedTab == 0) {
                Spacer(Modifier.height(12.dp))
                // Status filter (only for connections tab)
                GKFilterChips(
                    options = listOf("All", "Blocked", "Allowed", "System"),
                    selected = filterMode,
                    onSelect = { viewModel.setFilterMode(it) }
                )
            }
        }

        if (selectedTab == 0) {
            // ── Legend ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(AccentRed))
                    Spacer(Modifier.width(4.dp))
                    Text("Blocked by firewall/DNS", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(AccentGreen))
                    Spacer(Modifier.width(4.dp))
                    Text("Allowed", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(PrimaryCyan))
                    Spacer(Modifier.width(4.dp))
                    Text("System", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
            }

            // ── Connection List ───────────────────────────────────────────────
            if (connections.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Outlined.CloudOff, null, tint = TextTertiary, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No connections recorded", color = TextSecondary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            when (filterMode) {
                                "Blocked" -> "No blocked connections in the last $timeRange.\nAll traffic passed through the VPN cleanly."
                                "Allowed" -> "No allowed connections in the last $timeRange."
                                "System" -> "No VPN state change events recorded."
                                else -> "Enable the VPN to start monitoring network traffic."
                            },
                            color = TextTertiary,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Text(
                            "${connections.size} connection${if (connections.size != 1) "s" else ""} · $timeRange",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    items(connections, key = { "${it.timestamp}_${it.id}_${it.packageName}" }) { log ->
                        ConnectionLogItem(log, onClick = { selectedLog = log })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        } else {
            // ── Data Usage View ──────────────────────────────────────────────
            val usageList = bandwidthUsage.values.sortedByDescending { it.bytesIn + it.bytesOut }
            if (usageList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Outlined.DataUsage, null, tint = TextTertiary, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No data usage recorded", color = TextSecondary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Enable the VPN and use your apps to see real-time bandwidth usage statistics.",
                            color = TextTertiary,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "Real-time bandwidth since VPN started",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    items(usageList, key = { it.packageName }) { usage ->
                        DataUsageItem(usage)
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ── Detail Sheet ──────────────────────────────────────────────────────
    if (selectedLog != null) {
        val log = selectedLog!!
        ModalBottomSheet(
            onDismissRequest = { selectedLog = null },
            sheetState = sheetState,
            containerColor = DarkSurface
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp).fillMaxWidth()) {
                // Sheet header
                val accentColor = when {
                    log.isSystemEvent -> PrimaryCyan
                    log.wasBlocked -> AccentRed
                    else -> AccentGreen
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (log.isSystemEvent) Icons.Filled.Info
                            else if (log.wasBlocked) Icons.Filled.Block
                            else Icons.Filled.CheckCircle,
                            null, tint = accentColor, modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            if (log.isSystemEvent) "System Event" else log.appName ?: "Unknown App",
                            style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(accentColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                when {
                                    log.isSystemEvent -> "SYSTEM"
                                    log.wasBlocked -> "BLOCKED"
                                    else -> "ALLOWED"
                                },
                                style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = BorderDefault)
                Spacer(Modifier.height(16.dp))

                if (log.isSystemEvent) {
                    DetailRow("Event", log.systemEventReason ?: "Unknown", Icons.Filled.Info)
                } else {
                    val fmt = remember { SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault()) }
                    DetailRow("App", "${log.appName}\n${log.packageName}", Icons.Filled.Apps)
                    DetailRow("Remote IP", log.remoteIp, Icons.Filled.Router)
                    if (log.remoteHostname != null) DetailRow("Hostname", log.remoteHostname, Icons.Filled.Language)
                    DetailRow("Protocol", "${log.protocol} · Port ${log.remotePort}", Icons.Filled.Cable)
                    if (log.country != null) DetailRow("Country", log.country, Icons.Filled.Public)
                    DetailRow("Time", fmt.format(Date(log.timestamp)), Icons.Filled.Schedule)
                    if (log.bytesIn > 0 || log.bytesOut > 0) {
                        DetailRow("Data", "↓ ${formatBytes(log.bytesIn)}  ↑ ${formatBytes(log.bytesOut)}", Icons.Filled.SwapVert)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Action buttons
                    val context = LocalContext.current
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("IP", log.remoteIp))
                                selectedLog = null
                            },
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDefault)
                        ) {
                            Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                            Spacer(Modifier.width(6.dp))
                            Text("Copy IP", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        }
                        if (log.remoteHostname != null) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Host", log.remoteHostname))
                                    selectedLog = null
                                },
                                modifier = Modifier.weight(1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDefault)
                            ) {
                                Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                                Spacer(Modifier.width(6.dp))
                                Text("Copy Host", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = TextTertiary, modifier = Modifier.size(16.dp).padding(top = 2.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
            Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConnectionLogItem(log: ConnectionLog, onClick: () -> Unit) {
    val isBlocked = log.wasBlocked
    val isSystem = log.isSystemEvent
    val accentColor = when {
        isSystem -> PrimaryCyan
        isBlocked -> AccentRed
        else -> AccentGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .combinedClickable(onClick = onClick)
    ) {
        // Colored left border indicating status
        Box(modifier = Modifier.width(4.dp).height(64.dp).background(accentColor))

        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App/event icon
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isSystem -> Icons.Filled.Info
                        isBlocked -> Icons.Filled.Block
                        else -> Icons.Filled.Public
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isSystem) {
                    Text(
                        text = log.systemEventReason ?: "System Event",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text("VPN / GateKeeper", color = TextTertiary, style = MaterialTheme.typography.labelSmall)
                } else {
                    Text(
                        text = log.remoteHostname ?: log.remoteIp,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "${log.appName ?: log.packageName} · ${log.protocol}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Right side: time + status badge
            Column(horizontalAlignment = Alignment.End) {
                val format = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                Text(
                    format.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        when {
                            isSystem -> "SYS"
                            isBlocked -> "BLOCK"
                            else -> "OK"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DataUsageItem(usage: com.gatekeeper.mobile.vpn.AppBandwidth) {
    val context = LocalContext.current
    val pm = context.packageManager
    var appName by remember { mutableStateOf(usage.packageName) }
    var appIcon by remember { mutableStateOf<android.graphics.drawable.Drawable?>(null) }

    LaunchedEffect(usage.packageName) {
        try {
            val appInfo = pm.getApplicationInfo(usage.packageName, 0)
            appName = pm.getApplicationLabel(appInfo).toString()
            appIcon = pm.getApplicationIcon(appInfo)
        } catch (e: Exception) {
            // keep package name
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App icon
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                .background(AccentOrange.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            if (appIcon != null) {
                coil.compose.AsyncImage(model = appIcon, contentDescription = appName, modifier = Modifier.size(28.dp))
            } else {
                Icon(Icons.Filled.Apps, null, tint = AccentOrange, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                appName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ArrowDownward, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(formatBytes(usage.bytesIn), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                
                Spacer(Modifier.width(12.dp))
                
                Icon(Icons.Filled.ArrowUpward, null, tint = PrimaryCyan, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(formatBytes(usage.bytesOut), color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Total
        val total = usage.bytesIn + usage.bytesOut
        Text(
            formatBytes(total),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AccentOrange
        )
    }
}
