package com.gatekeeper.mobile.ui.screens.firewall

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.domain.model.InstalledApp
import com.gatekeeper.mobile.ui.components.GKEmptyState
import com.gatekeeper.mobile.ui.components.GKInfoButton
import com.gatekeeper.mobile.ui.components.GKInfoDialog
import com.gatekeeper.mobile.ui.theme.LocalGKColors
import com.gatekeeper.mobile.vpn.GateKeeperVpnService

@Composable
fun FirewallScreen(navController: NavController, viewModel: FirewallViewModel = hiltViewModel()) {
    val apps by viewModel.apps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val screenOffBlockedPkgs by viewModel.screenOffBlockedPkgs.collectAsState()
    val isVpnActive by GateKeeperVpnService.isRunning.collectAsState()
    val blockedCount by viewModel.blockedCount.collectAsState(initial = 0)

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf("All") } // "All", "Blocked", "Allowed"
    var showInfoDialog by remember { mutableStateOf(false) }

    val displayed = apps
        .filter {
            when (filterMode) {
                "Blocked" -> it.isBlocked
                "Allowed" -> !it.isBlocked
                else -> true
            }
        }
        .filter { it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }

    val primaryColor = Color(0xFF00D4FF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
    ) {
        // TopAppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Security, contentDescription = null, tint = primaryColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("GateKeeper", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = primaryColor)
        }

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // Header & Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("App Gate", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textPrimary)
                    GKInfoButton(color = primaryColor) { showInfoDialog = true }
                }
                Text("Control network access per application.", style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textSecondary, modifier = Modifier.padding(top = 4.dp))

                // VPN-Off Warning Banner
                AnimatedVisibility(
                    visible = !isVpnActive,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFB300).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFFFB300).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("VPN Inactive", style = MaterialTheme.typography.titleSmall, color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)
                                Text("Firewall rules configured but NOT enforced. Enable VPN from Dashboard.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFB300).copy(alpha = 0.8f))
                            }
                        }
                    }
                }

                // Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                Text("Search installed apps...", color = LocalGKColors.current.textSecondary.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium)
                            }
                            innerTextField()
                        }
                    )
                }

                // Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val allCount = apps.size
                    val allowedCount = allCount - blockedCount
                    FilterChipButton("All ($allCount)", filterMode == "All", primaryColor) { filterMode = "All" }
                    FilterChipButton("Blocked ($blockedCount)", filterMode == "Blocked", primaryColor) { filterMode = "Blocked" }
                    FilterChipButton("Allowed ($allowedCount)", filterMode == "Allowed", primaryColor) { filterMode = "Allowed" }
                }

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                } else if (displayed.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GKEmptyState(icon = Icons.Filled.SearchOff, title = "No Apps Found", subtitle = "Try adjusting your filters.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(displayed, key = { it.packageName }) { app ->
                            AppFirewallHtmlItem(
                                app = app,
                                isScreenOffBlocked = app.packageName in screenOffBlockedPkgs,
                                onToggle = { isAllowed -> viewModel.toggleBlock(app.packageName, app.appName, !isAllowed) },
                                onScreenOffToggle = { block -> viewModel.toggleScreenOffBlock(app.packageName, app.appName, block) }
                            )
                        }
                    }
                }
            }

        }

    if (showInfoDialog) {
        GKInfoDialog(
            title = "App Gate",
            body = "App Gate controls which apps on your phone can access the internet.\n\nToggle an app OFF to block it completely — it won't be able to send or receive any data, even in the background.\n\nThe \"Screen-off block\" option stops the app using internet while your screen is off, which saves battery and prevents background data leaks.",
            accentColor = primaryColor,
            onDismiss = { showInfoDialog = false }
        )
    }
    }
}

@Composable
fun FilterChipButton(text: String, isSelected: Boolean, primaryColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) primaryColor else Color.White.copy(alpha = 0.03f))
            .border(1.dp, if (isSelected) primaryColor else Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color(0xFF00586B) else LocalGKColors.current.textPrimary
        )
    }
}

@Composable
fun AppFirewallHtmlItem(
    app: InstalledApp,
    isScreenOffBlocked: Boolean,
    onToggle: (Boolean) -> Unit,
    onScreenOffToggle: (Boolean) -> Unit
) {
    val isAllowed = !app.isBlocked
    
    val statusColor = if (isAllowed) Color(0xFF37DF66) else LocalGKColors.current.accentRed
    val statusIcon = if (isAllowed) Icons.Filled.CheckCircle else Icons.Filled.Block
    val statusText = if (isAllowed) "Allowed" else "Blocked"

    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable { isExpanded = !isExpanded }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val appIcon = remember(app.packageName) {
                    try { context.packageManager.getApplicationIcon(app.packageName) } catch (e: Exception) { null }
                }

                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(statusColor.copy(alpha = 0.1f)).border(1.dp, statusColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        coil.compose.AsyncImage(model = appIcon, contentDescription = app.appName, modifier = Modifier.size(32.dp))
                    } else {
                        Icon(Icons.Filled.Apps, null, tint = statusColor, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(app.appName, style = MaterialTheme.typography.titleMedium, color = if (isAllowed) LocalGKColors.current.textPrimary else LocalGKColors.current.textPrimary.copy(alpha = 0.5f), maxLines = 1)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                HtmlToggleSwitch(
                    checked = isAllowed,
                    onCheckedChange = { onToggle(it) },
                    activeColor = Color(0xFF37DF66)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.05f)))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ScreenLockPortrait, null, tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Screen-off block", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                            Text(
                                "Blocks internet while your screen is off",
                                style = MaterialTheme.typography.labelSmall,
                                color = LocalGKColors.current.textSecondary.copy(alpha = 0.5f)
                            )
                        }
                    }
                    HtmlToggleSwitch(
                        checked = isScreenOffBlocked,
                        onCheckedChange = { onScreenOffToggle(it) },
                        activeColor = Color(0xFF37DF66),
                        modifier = Modifier.scale(0.75f)
                    )
                }
            }
        }
    }
}

@Composable
fun HtmlToggleSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, activeColor: Color, modifier: Modifier = Modifier) {
    val offsetX by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (checked) 24.dp else 2.dp,
        animationSpec = androidx.compose.animation.core.spring(stiffness = 400f),
        label = "toggle_offset"
    )
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(if (checked) activeColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
            .border(1.dp, if (checked) activeColor else Color.White.copy(alpha = 0.1f), RoundedCornerShape(13.dp))
            .clickable(role = androidx.compose.ui.semantics.Role.Switch, onClick = { onCheckedChange(!checked) })
    ) {
        Box(
            modifier = Modifier
                .offset(x = offsetX, y = 4.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(if (checked) activeColor else Color(0xFF859398))
        )
    }
}
