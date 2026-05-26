package com.gatekeeper.mobile.ui.screens.permissionauditor

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.data.db.dao.AppSensorUsage
import com.gatekeeper.mobile.data.db.dao.SensorSummary
import com.gatekeeper.mobile.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyDashboardScreen(
    navController: NavController,
    viewModel: PermissionAuditorViewModel = hiltViewModel()
) {
    val todaySummary by viewModel.todaySummary.collectAsState()
    val todayPerApp by viewModel.todayPerApp.collectAsState()
    val autoVpnStart by viewModel.autoVpnStart.collectAsState()

    val cameraStats = todaySummary.find { it.sensorType == "CAMERA" }
    val micStats = todaySummary.find { it.sensorType == "MICROPHONE" }
    val locationStats = todaySummary.find { it.sensorType == "LOCATION" }

    val totalAccesses = (cameraStats?.accessCount ?: 0) + (micStats?.accessCount ?: 0) + (locationStats?.accessCount ?: 0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ── Top Bar ─────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = TextSecondary)
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Privacy Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Today's hardware access log", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                // Live dot
                val pulse = rememberInfiniteTransition(label = "pulse")
                val alpha by pulse.animateFloat(0.4f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dot")
                Box(
                    modifier = Modifier.size(10.dp).clip(CircleShape)
                        .background(if (totalAccesses > 0) AccentOrange.copy(alpha = alpha) else AccentGreen.copy(alpha = alpha))
                )
            }
        }

        // ── VPN Auto-Start Card ──────────────────────────────────────────────
        item {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(PrimaryCyan.copy(0.12f), Color(0xFF1A1A2E))))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                        .background(PrimaryCyan.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.VpnKey, null, tint = PrimaryCyan, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Auto-Start VPN", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text(
                        if (autoVpnStart) "VPN starts automatically when you open GateKeeper"
                        else "VPN must be started manually each session",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                }
                Switch(
                    checked = autoVpnStart,
                    onCheckedChange = { viewModel.setAutoVpnStart(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = PrimaryCyan, checkedTrackColor = PrimaryCyan.copy(0.3f))
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Today At a Glance ────────────────────────────────────────────────
        item {
            Text(
                "Today's Hardware Access",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(10.dp))
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SensorHeroCard(
                    icon = Icons.Filled.CameraAlt,
                    label = "Camera",
                    count = cameraStats?.accessCount ?: 0,
                    durationMs = cameraStats?.totalDurationMs ?: 0L,
                    color = AccentRed,
                    modifier = Modifier.weight(1f)
                )
                SensorHeroCard(
                    icon = Icons.Filled.Mic,
                    label = "Microphone",
                    count = micStats?.accessCount ?: 0,
                    durationMs = micStats?.totalDurationMs ?: 0L,
                    color = AccentOrange,
                    modifier = Modifier.weight(1f)
                )
                SensorHeroCard(
                    icon = Icons.Filled.LocationOn,
                    label = "Location",
                    count = locationStats?.accessCount ?: 0,
                    durationMs = locationStats?.totalDurationMs ?: 0L,
                    color = PrimaryCyan,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Per-App Breakdown ────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "App Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (todayPerApp.isNotEmpty()) {
                    Text(
                        "${todayPerApp.size} apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (todayPerApp.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.PrivacyTip, null, tint = AccentGreen.copy(0.5f), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No hardware accessed today", style = MaterialTheme.typography.bodyLarge, color = TextSecondary, fontWeight = FontWeight.Medium)
                    Text("Camera, mic and location have not been used by any app today.", style = MaterialTheme.typography.bodySmall, color = TextTertiary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 40.dp, vertical = 4.dp))
                }
            }
        } else {
            items(todayPerApp, key = { "${it.packageName}_${it.sensorType}" }) { usage ->
                AppSensorRow(usage = usage)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun SensorHeroCard(
    icon: ImageVector,
    label: String,
    count: Int,
    durationMs: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(color.copy(0.12f), DarkCard)))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (count > 0) color else TextTertiary
        )
        Text("times", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatDuration(durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextTertiary)
    }
}

@Composable
fun AppSensorRow(usage: AppSensorUsage) {
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val (icon, color) = when (usage.sensorType) {
        "CAMERA" -> Icons.Filled.CameraAlt to AccentRed
        "MICROPHONE" -> Icons.Filled.Mic to AccentOrange
        "LOCATION" -> Icons.Filled.LocationOn to PrimaryCyan
        else -> Icons.Filled.Sensors to TextTertiary
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sensor icon
        Box(
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))

        // App name + details
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    usage.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                if (usage.hadBackground == 1) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(AccentRed.copy(0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("BACKGROUND", style = MaterialTheme.typography.labelSmall, color = AccentRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "${usage.accessCount}x · ${formatDuration(usage.totalDurationMs)} · Last at ${fmt.format(Date(usage.lastAccessAt))}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        // Usage bar
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp))
                .background(color.copy(0.15f))
        ) {
            val fillFraction = (usage.accessCount.toFloat() / 10f).coerceIn(0.1f, 1f)
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(fillFraction)
                    .clip(RoundedCornerShape(2.dp)).background(color).align(Alignment.BottomCenter)
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0s"
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}
