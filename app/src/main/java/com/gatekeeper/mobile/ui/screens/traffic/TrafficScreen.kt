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
    // Limit to 100 items max to prevent LazyColumn from working on huge lists.
    val allConnections by viewModel.recentConnections.collectAsState(initial = emptyList())
    val connections = remember(allConnections) { allConnections.take(100) }

    val totalCount by viewModel.totalConnections.collectAsState(initial = 0)
    val blockedCount = remember(connections) { connections.count { it.wasBlocked } }

    val timeRange by viewModel.timeRange.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()

    var selectedLog by remember { mutableStateOf<ConnectionLog?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // ── Header ────────────────────────────────────────────────────────────
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
                    Text("Real-time network activity", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Spacer(Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            if (totalCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PulsingDots(color = AccentGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Receiving live traffic...", style = MaterialTheme.typography.labelSmall, color = AccentGreen)
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Total Events", "$totalCount", gradientColors = listOf(AccentGreen, PrimaryCyan), modifier = Modifier.weight(1f))
                StatCard("Blocked", "$blockedCount", gradientColors = GradientDanger, modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(16.dp))

            // Filters
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                GKFilterChips(
                    options = listOf("1h", "24h", "7d"),
                    selected = timeRange,
                    onSelect = { viewModel.setTimeRange(it) }
                )
            }
            Spacer(Modifier.height(8.dp))
            GKFilterChips(
                options = listOf("All", "Blocked", "Allowed", "System"),
                selected = filterMode,
                onSelect = { viewModel.setFilterMode(it) }
            )
        }

        // ── List ──────────────────────────────────────────────────────────────
        if (connections.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.CloudOff, null, tint = TextTertiary, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No traffic recorded yet", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("Adjust filters or turn on VPN", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(connections, key = { "${it.timestamp}_${it.id}_${it.packageName}" }) { log ->
                    ConnectionLogItem(log, onClick = { selectedLog = log })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (selectedLog != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedLog = null },
            sheetState = sheetState,
            containerColor = DarkSurface
        ) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                Text("Connection Details", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Spacer(Modifier.height(16.dp))
                
                if (selectedLog!!.isSystemEvent) {
                    Text("Event: ${selectedLog!!.systemEventReason}", color = TextSecondary)
                } else {
                    Text("App: ${selectedLog!!.appName}", color = TextSecondary)
                    Text("Package: ${selectedLog!!.packageName}", color = TextSecondary)
                    Text("Remote IP: ${selectedLog!!.remoteIp}", color = TextSecondary)
                    Text("Remote Host: ${selectedLog!!.remoteHostname ?: "Unknown"}", color = TextSecondary)
                    Text("Protocol: ${selectedLog!!.protocol}", color = TextSecondary)
                    Text("Port: ${selectedLog!!.remotePort}", color = TextSecondary)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
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

    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        // Colored left border
        Box(modifier = Modifier.width(4.dp).height(66.dp).background(accentColor))

        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(11.dp))
                    .background(accentColor.copy(alpha = 0.1f)),
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
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isSystem) {
                    Text(
                        text = log.systemEventReason ?: "System Event",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Text("GateKeeper VPN", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(
                        text = log.remoteHostname ?: log.remoteIp,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = log.appName ?: log.packageName ?: "Unknown App",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(" • ${log.protocol}", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

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
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        when {
                            isSystem -> "SYSTEM"
                            isBlocked -> "BLOCKED"
                            else -> "ALLOWED"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(DarkSurfaceVariant)
            ) {
                DropdownMenuItem(
                    text = { Text("Copy IP", color = TextPrimary) },
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("IP", log.remoteIp))
                        showMenu = false
                    }
                )
                if (log.remoteHostname != null) {
                    DropdownMenuItem(
                        text = { Text("Copy Hostname", color = TextPrimary) },
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Hostname", log.remoteHostname))
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
