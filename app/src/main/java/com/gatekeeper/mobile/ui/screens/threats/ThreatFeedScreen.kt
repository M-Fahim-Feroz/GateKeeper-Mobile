package com.gatekeeper.mobile.ui.screens.threats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.theme.LocalGKColors
import com.gatekeeper.mobile.ui.components.GKInfoButton
import com.gatekeeper.mobile.ui.components.GKInfoDialog
import kotlinx.coroutines.launch

@Composable
fun ThreatFeedScreen(
    navController: NavController? = null,
    viewModel: ThreatFeedViewModel = hiltViewModel()
) {
    val allThreats by viewModel.allThreats.collectAsState(initial = emptyList())
    val subscriptions by viewModel.subscriptions.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState()
    val importStatus by viewModel.importStatus.collectAsState()
    
    val importedFeedNames = remember(subscriptions) { subscriptions.map { it.name }.toSet() }
    var showImportMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showAddFeedDialog by remember { mutableStateOf(false) }

    val primaryColor = Color(0xFF00D4FF)

    Column(modifier = Modifier.fillMaxSize().background(LocalGKColors.current.background)) {
        // TopAppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalGKColors.current.background)
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
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 24.dp, bottom = 120.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header & Primary Action
            item {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Threat Intel", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text("Global intelligence feeds and active interception monitoring.", fontSize = 14.sp, color = LocalGKColors.current.textSecondary)
                        }
                        GKInfoButton(color = primaryColor) { showInfoDialog = true }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(primaryColor)
                                .clickable {
                                    if (showImportMenu) viewModel.importAllRecommended() else showImportMenu = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AddBox, contentDescription = null, tint = Color(0xFF001F27), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(if (showImportMenu) "Import All" else "Import Feed", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF001F27))
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable { showAddFeedDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Add Custom", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                    
                    if (isLoading || importStatus != null) {
                        Text(importStatus ?: "Loading...", fontSize = 14.sp, color = primaryColor)
                    }
                }
            }

            // Active Feeds Grid
            item {
                Text("Active Feeds", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(12.dp))
                
                val availableFeeds = viewModel.recommendedFeeds.filter { !importedFeedNames.contains(it.name) }
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (subscriptions.isEmpty()) {
                        Text("No active feeds. Import a feed below to start blocking threats.", color = LocalGKColors.current.textSecondary, modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        subscriptions.forEach { sub ->
                            FilterListCardThreat(subscription = sub, color = primaryColor, viewModel = viewModel)
                        }
                    }
                    
                    if (showImportMenu && availableFeeds.isNotEmpty()) {
                        Text("Available Feeds", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 12.dp))
                        availableFeeds.forEach { feed ->
                            FeedCardReal(feed.name, feed.description, false) { viewModel.importFeed(feed) }
                        }
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        GKInfoDialog(
            title = "Threat Intel",
            body = "Threat Intel downloads lists of known malicious websites, hacker command servers, and malware sources from trusted security organisations worldwide.\n\nAny time an app on your phone tries to contact one of these servers, the connection is automatically blocked.\n\nThe lists update automatically so you're always protected against the latest threats.",
            accentColor = primaryColor,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showAddFeedDialog) {
        var feedName by remember { mutableStateOf("") }
        var feedUrl by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }
        val isValid = feedName.trim().isNotBlank() &&
            (feedUrl.trim().startsWith("http://") || feedUrl.trim().startsWith("https://"))

        AlertDialog(
            onDismissRequest = { showAddFeedDialog = false },
            title = { Text("Add Custom Threat Feed") },
            text = {
                Column {
                    OutlinedTextField(
                        value = feedName, onValueChange = { feedName = it },
                        label = { Text("Feed Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = LocalGKColors.current.textSecondary,
                            focusedTextColor = LocalGKColors.current.textPrimary,
                            unfocusedTextColor = LocalGKColors.current.textPrimary
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = feedUrl,
                        onValueChange = { feedUrl = it; isError = false },
                        label = { Text("URL (https://...)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        isError = isError,
                        supportingText = if (isError) { { Text("Must start with http:// or https://") } } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedLabelColor = primaryColor,
                            unfocusedLabelColor = LocalGKColors.current.textSecondary,
                            focusedTextColor = LocalGKColors.current.textPrimary,
                            unfocusedTextColor = LocalGKColors.current.textPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isValid) {
                            viewModel.importFeed(
                                ThreatFeedViewModel.FeedSource(
                                    name = feedName.trim(),
                                    url = feedUrl.trim(),
                                    type = "domain",
                                    threatType = "custom",
                                    description = "Custom user-added feed."
                                )
                            )
                            showAddFeedDialog = false
                        } else { isError = true }
                    },
                    enabled = isValid
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddFeedDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun FilterListCardThreat(subscription: com.gatekeeper.mobile.data.db.entity.BlocklistSubscription, color: Color, viewModel: ThreatFeedViewModel) {
    val dateFormatter = remember { java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()) }
    val lastUpdatedText = if (subscription.lastRefreshedAt > 0) dateFormatter.format(java.util.Date(subscription.lastRefreshedAt)) else "Never"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.1f))
                        .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Shield, null, tint = color)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(subscription.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = LocalGKColors.current.textPrimary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(subscription.url, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            Switch(
                checked = subscription.isEnabled,
                onCheckedChange = { viewModel.toggleSubscription(subscription, it) },
                colors = SwitchDefaults.colors(checkedThumbColor = color, checkedTrackColor = color.copy(alpha = 0.3f))
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.2f)).border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("${subscription.domainCount} Rules", style = MaterialTheme.typography.labelSmall, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                val statusColor = when (subscription.fetchStatus) {
                    "SUCCESS" -> LocalGKColors.current.accentGreen
                    "FAILED" -> LocalGKColors.current.accentRed
                    "FETCHING" -> LocalGKColors.current.accentYellow
                    else -> LocalGKColors.current.textTertiary
                }
                Text(if (subscription.fetchStatus == "FAILED") "Failed: ${subscription.errorReason ?: "Unknown"}" else "Sync: $lastUpdatedText", style = MaterialTheme.typography.labelSmall, color = statusColor, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Row {
                IconButton(onClick = { viewModel.toggleSubscription(subscription, true) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Refresh, null, tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { viewModel.removeFeed(subscription) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.DeleteOutline, null, tint = LocalGKColors.current.accentRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun FilterButton(label: String, isSelected: Boolean = false, color: Color? = null, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    val bgColor = if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent
    val borderColor = color?.copy(alpha = 0.3f) ?: Color.White.copy(alpha = 0.2f)
    val textColor = color ?: if (isSelected) Color.White else LocalGKColors.current.textSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor, letterSpacing = 0.5.sp)
        }
    }
}


@Composable
fun FeedCardReal(title: String, desc: String, isActive: Boolean, onImport: (() -> Unit)? = null) {
    val borderColor = Color.White.copy(alpha = 0.1f)
    val bgColor = Color.White.copy(alpha = 0.05f)
    val iconColor = LocalGKColors.current.textSecondary
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onImport?.invoke() }
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(iconColor.copy(alpha = 0.1f)).border(1.dp, iconColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Shield, null, tint = iconColor)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text(desc, fontSize = 12.sp, color = LocalGKColors.current.textSecondary)
                }
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        if (isActive) {
            val pillColor = Color(0xFF5BFC80)
            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(pillColor.copy(alpha = 0.1f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(pillColor))
                    Spacer(Modifier.width(4.dp))
                    Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = pillColor, letterSpacing = 1.sp)
                }
            }
        } else {
            Icon(Icons.Filled.AddCircleOutline, null, tint = Color(0xFF00D4FF))
        }
    }
}

