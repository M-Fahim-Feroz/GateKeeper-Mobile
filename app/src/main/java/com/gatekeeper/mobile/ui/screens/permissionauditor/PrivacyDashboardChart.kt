package com.gatekeeper.mobile.ui.screens.permissionauditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gatekeeper.mobile.data.db.entity.SensorLog
import com.gatekeeper.mobile.ui.theme.*

@Composable
fun PrivacyDashboardChart(logs: List<SensorLog>) {
    val recentLogs = logs.filter { System.currentTimeMillis() - it.startedAt < 24 * 60 * 60 * 1000 }
    
    val cameraCount = recentLogs.filter { it.sensorType == "CAMERA" }.distinctBy { it.packageName }.size
    val micCount = recentLogs.filter { it.sensorType == "MICROPHONE" }.distinctBy { it.packageName }.size
    val locationCount = recentLogs.filter { it.sensorType == "LOCATION" }.distinctBy { it.packageName }.size
    val totalCount = cameraCount + micCount + locationCount

    if (totalCount == 0) {
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp)).background(DarkCard),
            contentAlignment = Alignment.Center
        ) {
            Text("No sensor activity in the past 24 hours.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val cameraSweep = (cameraCount.toFloat() / totalCount) * 360f
    val micSweep = (micCount.toFloat() / totalCount) * 360f
    val locationSweep = (locationCount.toFloat() / totalCount) * 360f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .padding(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 24f
                var startAngle = -90f

                // Draw Camera
                if (cameraCount > 0) {
                    drawArc(
                        color = AccentRed,
                        startAngle = startAngle,
                        sweepAngle = cameraSweep - 2f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += cameraSweep
                }
                
                // Draw Mic
                if (micCount > 0) {
                    drawArc(
                        color = AccentOrange,
                        startAngle = startAngle,
                        sweepAngle = micSweep - 2f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                    startAngle += micSweep
                }

                // Draw Location
                if (locationCount > 0) {
                    drawArc(
                        color = AccentYellow,
                        startAngle = startAngle,
                        sweepAngle = locationSweep - 2f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Past", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("24 hours", style = MaterialTheme.typography.labelMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.width(32.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (cameraCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AccentRed))
                    Spacer(Modifier.width(8.dp))
                    Text("Camera", color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            if (micCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AccentOrange))
                    Spacer(Modifier.width(8.dp))
                    Text("Microphone", color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            if (locationCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AccentYellow))
                    Spacer(Modifier.width(8.dp))
                    Text("Location", color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
