package com.gatekeeper.mobile.ui.screens.traffic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.data.db.entity.ConnectionLog
import com.gatekeeper.mobile.ui.components.GKInfoButton
import com.gatekeeper.mobile.ui.components.GKInfoDialog
import com.gatekeeper.mobile.ui.theme.LocalGKColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PulsingDot(color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.matchParentSize().scale(scale).clip(CircleShape).background(color.copy(alpha = alpha * 0.5f)))
        Box(modifier = Modifier.matchParentSize().clip(CircleShape).background(color))
    }
}

@Composable
fun TrafficScreen(navController: NavController, viewModel: TrafficViewModel = hiltViewModel()) {
    val allConnections by viewModel.recentConnections.collectAsState(initial = emptyList())
    val context = androidx.compose.ui.platform.LocalContext.current
    val connections = remember(allConnections) { allConnections.sortedByDescending { it.timestamp }.take(200) }
    
    var searchQuery by remember { mutableStateOf("") }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showCsvInfo by remember { mutableStateOf(false) }
    var showPcapInfo by remember { mutableStateOf(false) }
    
    val displayed = connections.filter { 
        (it.appName ?: "").contains(searchQuery, ignoreCase = true) || 
        it.destinationIp.contains(searchQuery, ignoreCase = true) ||
        (it.hostname ?: "").contains(searchQuery, ignoreCase = true)
    }

    val primaryColor = Color(0xFF00D4FF)
    val bgColor = Color(0xFF05070A)

    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // TopAppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor.copy(alpha = 0.8f))
                .border(1.dp, Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = primaryColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("GateKeeper", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = primaryColor)
            }
        }

        // Main Content Area
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
            
            // Header & Actions
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Traffic Monitor", style = MaterialTheme.typography.headlineMedium, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            PulsingDot(color = primaryColor, modifier = Modifier.size(8.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Live Connection Log", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                        }
                    }
                    GKInfoButton(color = primaryColor) { showInfoDialog = true }
                }
                
                // Export Options
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Export CSV
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { viewModel.exportCsv(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Download, null, tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export CSV", fontSize = 14.sp, color = LocalGKColors.current.textSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.Info, null, tint = LocalGKColors.current.textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(13.dp).clickable { showCsvInfo = true })
                        }
                    }

                    // PCAP (Glow Active)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryColor.copy(alpha = 0.1f))
                            .border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { viewModel.exportPcap(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Download, null, tint = primaryColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PCAP", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.Info, null, tint = primaryColor.copy(alpha = 0.6f), modifier = Modifier.size(13.dp).clickable { showPcapInfo = true })
                        }
                    }
                }
            }

            // Filter/Search Bar (glass-card)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, null, tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = LocalGKColors.current.textPrimary),
                    cursorBrush = SolidColor(primaryColor),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("Filter by IP, domain, or app...", color = LocalGKColors.current.textSecondary.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium)
                        }
                        innerTextField()
                    },
                    singleLine = true
                )
                Icon(Icons.Filled.FilterList, null, tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(20.dp).clickable {})
            }

            // Traffic List
            if (displayed.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No connections to display", color = LocalGKColors.current.textSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(displayed, key = { "${it.timestamp}_${it.id}_${it.packageName}" }) { log ->
                        ConnectionLogHtmlItem(log)
                        if (log != displayed.last()) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        GKInfoDialog(
            title = "NetWatch",
            body = "NetWatch records every network connection your apps make in real time.\n\nYou can see which app connected, where it went, how much data was sent, and whether it was blocked.\n\nThink of it as a phone bill for your internet traffic — but live and with full details.",
            accentColor = primaryColor,
            onDismiss = { showInfoDialog = false }
        )
    }
    if (showCsvInfo) {
        GKInfoDialog(
            title = "Export CSV",
            body = "Saves a spreadsheet (.csv file) of all recorded connections to your Downloads folder.\n\nYou can open it in Excel or Google Sheets to review your traffic history at any time.",
            accentColor = primaryColor,
            onDismiss = { showCsvInfo = false }
        )
    }
    if (showPcapInfo) {
        GKInfoDialog(
            title = "PCAP Export",
            body = "Saves a raw packet capture file that can be opened in tools like Wireshark.\n\nThis is intended for advanced users and security researchers who want to inspect individual network packets.",
            accentColor = primaryColor,
            onDismiss = { showPcapInfo = false }
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

@Composable
fun ConnectionLogHtmlItem(log: ConnectionLog) {
    var expanded by remember { mutableStateOf(false) }
    
    val isBlocked = log.wasBlocked
    val isSystem = log.isSystemEvent
    
    val accentColor = when {
        isSystem -> Color(0xFF00D4FF) // Primary
        isBlocked -> Color(0xFFFFB4AB) // Error variant
        else -> Color(0xFF5BFC80) // Tertiary/Allowed
    }
    
    val statusIcon = when {
        isSystem -> Icons.Filled.Info
        isBlocked -> Icons.Filled.PublicOff
        else -> Icons.Filled.CheckCircle
    }

    val statusText = when {
        isSystem -> "SYSTEM"
        isBlocked -> "BLOCKED"
        else -> "ALLOWED"
    }

    val pillIcon = when {
        isSystem -> Icons.Filled.Info
        isBlocked -> Icons.Filled.Block
        else -> Icons.Filled.CheckCircle
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (expanded) Color.White.copy(alpha = 0.05f) else Color.Transparent)
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(statusIcon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = log.serviceName ?: log.hostname ?: log.destinationIp,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = LocalGKColors.current.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (log.serviceName != null) {
                            Spacer(Modifier.width(8.dp))
                            val confColor = if (log.serviceConfidence == com.gatekeeper.mobile.data.db.entity.ConfidenceLevel.HIGH) Color(0xFF5BFC80) else Color.Yellow
                            Icon(Icons.Filled.VerifiedUser, null, tint = confColor, modifier = Modifier.size(14.dp))
                        }
                    }
                    Text(
                        text = "${log.appName} (${log.packageName ?: "Unknown"}) • ${log.destinationIp}",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalGKColors.current.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 16.dp)) {
                    Text(formatBytes(log.bytesSent + log.bytesReceived), style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                    Text(log.country ?: "Unknown", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textSecondary, modifier = Modifier.padding(top = 4.dp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor.copy(alpha = 0.1f))
                        .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(pillIcon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(statusText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentColor, letterSpacing = 0.5.sp)
                    }
                }
            }
        }

        // Expandable Details
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 56.dp)
            ) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(bottom = 16.dp))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    val format = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                    
                    HtmlDetailBlock("Protocol", "${log.protocol}", modifier = Modifier.weight(1f))
                    HtmlDetailBlock("Source", "${log.sourceIp}:${log.sourcePort}", modifier = Modifier.weight(1.5f))
                    HtmlDetailBlock("Dest", "${log.destinationIp}:${log.destinationPort}", modifier = Modifier.weight(1.5f))
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    val format = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                    HtmlDetailBlock("Timestamp", format.format(Date(log.timestamp)), modifier = Modifier.weight(1f))
                    
                    val ruleName = if (isBlocked) "Ad/Tracker Blocklist" else "Default Allow"
                    HtmlDetailBlock("Rule", ruleName, color = accentColor, modifier = Modifier.weight(1f))
                    
                    HtmlDetailBlock("Confidence", log.serviceConfidence.name, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun HtmlDetailBlock(label: String, value: String, modifier: Modifier = Modifier, color: Color = LocalGKColors.current.textPrimary) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textSecondary, letterSpacing = 0.5.sp, modifier = Modifier.padding(bottom = 4.dp))
        Text(value, fontSize = 14.sp, color = color)
    }
}
