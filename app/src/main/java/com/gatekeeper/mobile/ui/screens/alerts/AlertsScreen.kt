package com.gatekeeper.mobile.ui.screens.alerts

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.data.db.entity.SecurityAlert
import com.gatekeeper.mobile.ui.theme.LocalGKColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AlertsScreen(
    navController: NavController,
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val alerts by viewModel.unresolvedAlerts.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val bgColor = Color(0xFF05070A)
    val primaryColor = Color(0xFF00D4FF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "System Alerts",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Active threat detections requiring attention.",
                        fontSize = 16.sp,
                        color = LocalGKColors.current.textSecondary
                    )
                }
            }

            if (alerts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active alerts", color = LocalGKColors.current.textSecondary)
                    }
                }
            } else {
                items(alerts, key = { it.id }) { alert ->
                    AlertItem(alert = alert, onResolve = {
                        coroutineScope.launch {
                            viewModel.resolveAlert(alert.id)
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun AlertItem(alert: SecurityAlert, onResolve: () -> Unit) {
    val (color, icon) = when (alert.severity.uppercase()) {
        "CRITICAL" -> Color(0xFFFFB4AB) to Icons.Filled.WifiTetheringError
        "HIGH" -> Color(0xFFFFB300) to Icons.Filled.CellTower
        "MEDIUM" -> Color(0xFFFF9800) to Icons.Filled.Warning
        else -> Color(0xFF00D4FF) to Icons.Filled.Info
    }

    val todayDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    val alertDayOfYear = Calendar.getInstance().apply { timeInMillis = alert.timestamp }.get(Calendar.DAY_OF_YEAR)
    val isToday = todayDayOfYear == alertDayOfYear
    val timeFormat = remember(isToday) {
        if (isToday) SimpleDateFormat("HH:mm", Locale.getDefault())
        else SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    }
    val timeString = timeFormat.format(Date(alert.timestamp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
    ) {
        // Glow effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(color.copy(alpha = 0.05f)) // Subtle gradient/glow
        )

        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = color
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.Info, contentDescription = null, tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = timeString,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalGKColors.current.textSecondary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = alert.description,
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .clickable(onClick = onResolve)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Dismiss", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                            .clickable(onClick = onResolve) // Same action for now, normally would be "Block", "Kill", etc. based on type
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Resolve", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (alert.severity.uppercase() == "CRITICAL") Color(0xFF690005) else Color.Black)
                    }
                }
            }
        }
    }
}
