package com.gatekeeper.mobile.ui.screens.traffic

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gatekeeper.mobile.data.db.entity.ConnectionLog
import com.gatekeeper.mobile.ui.components.*
import com.gatekeeper.mobile.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrafficScreen(viewModel: TrafficViewModel = hiltViewModel()) {
    // Limit to 100 items max to prevent LazyColumn from working on huge lists.
    // collectAsState with Dispatchers.Main is safe — Room delivers on main thread.
    val allConnections by viewModel.recentConnections.collectAsState(initial = emptyList())
    val connections = remember(allConnections) { allConnections.take(100) }

    val totalCount by viewModel.totalConnections.collectAsState(initial = 0)
    val blockedCount = remember(connections) { connections.count { it.wasBlocked } }

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
                StatCard("Total Events", "$totalCount", listOf(AccentGreen, PrimaryCyan), Modifier.weight(1f))
                StatCard("Blocked", "$blockedCount", GradientDanger, Modifier.weight(1f))
            }
        }

        // ── List ──────────────────────────────────────────────────────────────
        if (connections.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.CloudOff, null, tint = TextTertiary, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No traffic recorded yet", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("Turn on the VPN to monitor traffic", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // key must be stable — use timestamp + packageName to avoid recomposition
                items(connections, key = { "${it.timestamp}_${it.packageName}_${it.localPort}" }) { log ->
                    ConnectionLogItem(log)
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun ConnectionLogItem(log: ConnectionLog) {
    val isBlocked = log.wasBlocked
    val accentColor = if (isBlocked) AccentRed else AccentGreen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        // Simple icon — NO packageManager.getApplicationIcon() call on UI thread!
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(11.dp))
                .background(accentColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isBlocked) Icons.Filled.Block else Icons.Filled.Public,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
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
                    if (isBlocked) "BLOCKED" else "ALLOWED",
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
