package com.gatekeeper.mobile.ui.screens.firewall

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gatekeeper.mobile.domain.model.InstalledApp
import com.gatekeeper.mobile.ui.components.*
import com.gatekeeper.mobile.ui.theme.*
import com.gatekeeper.mobile.vpn.GateKeeperVpnService

@Composable
fun FirewallScreen(viewModel: FirewallViewModel = hiltViewModel()) {
    val apps by viewModel.apps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val excludedCount by viewModel.blockedCount.collectAsState(initial = 0)
    val isVpnActive by GateKeeperVpnService.isRunning.collectAsState()
    val screenOffBlockedPkgs by viewModel.screenOffBlockedPkgs.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showExcludedOnly by remember { mutableStateOf(false) }
    var showAllowedOnly by remember { mutableStateOf(false) }

    val displayed = apps
        .filter { when {
            showExcludedOnly -> it.isBlocked
            showAllowedOnly  -> !it.isBlocked
            else             -> true
        }}
        .filter { it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = Modifier.fillMaxSize().background(LocalGKColors.current.background)
    ) {
        // Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(LocalGKColors.current.primary.copy(alpha = 0.06f), LocalGKColors.current.background)))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(GradientPrimary.map { it.copy(alpha = 0.2f) })),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Shield, null, tint = LocalGKColors.current.primary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("App Firewall", style = MaterialTheme.typography.displaySmall, color = LocalGKColors.current.textPrimary)
                    Text("$excludedCount apps blocked from internet", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // VPN warning banner
            if (!isVpnActive) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(LocalGKColors.current.accentOrange.copy(alpha = 0.7f)))
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search installed apps...", color = LocalGKColors.current.textTertiary) },
                leadingIcon = { Icon(Icons.Filled.Search, "Search", tint = LocalGKColors.current.textTertiary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, "Clear search", tint = LocalGKColors.current.textTertiary)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LocalGKColors.current.primary,
                    unfocusedBorderColor = LocalGKColors.current.border,
                    focusedContainerColor = LocalGKColors.current.card,
                    unfocusedContainerColor = LocalGKColors.current.card,
                    focusedTextColor = LocalGKColors.current.textPrimary,
                    unfocusedTextColor = LocalGKColors.current.textPrimary
                ),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            // VPN OS Limitation Warning
            androidx.compose.animation.AnimatedVisibility(visible = excludedCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(LocalGKColors.current.accentOrange.copy(alpha = 0.1f))
                        .padding(16.dp)
                ) {
                    Row {
                        Icon(Icons.Filled.WarningAmber, null, tint = LocalGKColors.current.accentOrange, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Global DNS Filter Paused", style = MaterialTheme.typography.titleSmall, color = LocalGKColors.current.accentOrange, fontWeight = FontWeight.Bold)
                            Text(
                                "Due to Android OS limitations, when the App Firewall is active, all unblocked apps must bypass the VPN entirely to maintain internet speed. The DNS Filter will only resume when all apps are unblocked.",
                                style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary, modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showExcludedOnly && !showAllowedOnly,
                    onClick = { showExcludedOnly = false; showAllowedOnly = false },
                    label = { Text("All (${apps.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LocalGKColors.current.primary.copy(alpha = 0.15f),
                        selectedLabelColor = LocalGKColors.current.primary
                    )
                )
                FilterChip(
                    selected = showExcludedOnly,
                    onClick = { showExcludedOnly = true; showAllowedOnly = false },
                    label = { Text("Blocked ($excludedCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LocalGKColors.current.accentRed.copy(alpha = 0.15f),
                        selectedLabelColor = LocalGKColors.current.accentRed
                    )
                )
                FilterChip(
                    selected = showAllowedOnly,
                    onClick = { showAllowedOnly = true; showExcludedOnly = false },
                    label = { Text("Allowed (${apps.size - excludedCount})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LocalGKColors.current.accentGreen.copy(alpha = 0.15f),
                        selectedLabelColor = LocalGKColors.current.accentGreen
                    )
                )
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = LocalGKColors.current.primary, strokeWidth = 2.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading installed apps...", color = LocalGKColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (displayed.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GKEmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = "No Apps Found",
                    subtitle = "Try adjusting your filters or search query."
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(displayed, key = { it.packageName }) { app ->
                    AppFirewallItem(
                        app = app,
                        isScreenOffBlocked = app.packageName in screenOffBlockedPkgs,
                        onToggle = { excluded -> viewModel.toggleBlock(app.packageName, app.appName, excluded) },
                        onScreenOffToggle = { block -> viewModel.toggleScreenOffBlock(app.packageName, app.appName, block) },
                        onScheduleUpdate = { enabled, start, end -> viewModel.updateSchedule(app.packageName, app.appName, enabled, start, end) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFirewallItem(
    app: InstalledApp,
    isScreenOffBlocked: Boolean,
    onToggle: (Boolean) -> Unit,
    onScreenOffToggle: (Boolean) -> Unit,
    onScheduleUpdate: (Boolean, Int, Int) -> Unit
) {
    val excluded = app.isBlocked
    var isExpanded by remember { mutableStateOf(false) }

    val shape = RoundedCornerShape(14.dp)
    val borderBrush = if (excluded)
        Brush.horizontalGradient(GradientOrange.map { it.copy(alpha = 0.4f) })
    else
        Brush.horizontalGradient(listOf(LocalGKColors.current.border, LocalGKColors.current.border))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LocalGKColors.current.card)
            .clickable { isExpanded = !isExpanded }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val appIcon = remember(app.packageName) {
                try { context.packageManager.getApplicationIcon(app.packageName) } catch (e: Exception) { null }
            }

            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(11.dp))
                        .background(if (excluded) LocalGKColors.current.accentOrange.copy(alpha = 0.12f) else LocalGKColors.current.primary.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        coil.compose.AsyncImage(model = appIcon, contentDescription = app.appName, modifier = Modifier.size(28.dp))
                    } else {
                        Icon(Icons.Filled.Apps, null, tint = if (excluded) LocalGKColors.current.accentOrange else LocalGKColors.current.primary, modifier = Modifier.size(22.dp))
                    }
                }
                // Risk dot overlay
                if (app.sensitivePermCount >= 3) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(LocalGKColors.current.accentRed))
                } else if (app.sensitivePermCount >= 1) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(LocalGKColors.current.accentOrange))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = LocalGKColors.current.textPrimary, maxLines = 1)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textTertiary, maxLines = 1)
            }

            // Internet Block toggle
            Switch(
                checked = excluded,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = LocalGKColors.current.accentRed,
                    checkedTrackColor = LocalGKColors.current.accentRed.copy(alpha = 0.25f),
                    uncheckedThumbColor = LocalGKColors.current.accentGreen.copy(alpha = 0.8f),
                    uncheckedTrackColor = LocalGKColors.current.accentGreen.copy(alpha = 0.15f)
                )
            )
        }

        // F8: Screen-off blocking sub-row
        androidx.compose.animation.AnimatedVisibility(visible = excluded || isScreenOffBlocked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 54.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.NightlightRound, null,
                        tint = if (isScreenOffBlocked) LocalGKColors.current.accentOrange else LocalGKColors.current.textTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Block when screen off",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isScreenOffBlocked) LocalGKColors.current.accentOrange else LocalGKColors.current.textTertiary
                    )
                }
                Switch(
                    checked = isScreenOffBlocked,
                    onCheckedChange = { onScreenOffToggle(it) },
                    modifier = Modifier.size(width = 40.dp, height = 24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LocalGKColors.current.accentOrange,
                        checkedTrackColor = LocalGKColors.current.accentOrange.copy(alpha = 0.25f),
                        uncheckedThumbColor = LocalGKColors.current.textTertiary,
                        uncheckedTrackColor = LocalGKColors.current.surface
                    )
                )
            }
        }

        // Schedule sub-row
        androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                HorizontalDivider(color = LocalGKColors.current.border, modifier = Modifier.padding(bottom = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Schedule, null,
                            tint = if (app.blockScheduleEnabled) LocalGKColors.current.primary else LocalGKColors.current.textTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "Time Schedule",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (app.blockScheduleEnabled) LocalGKColors.current.textPrimary else LocalGKColors.current.textSecondary
                            )
                            if (app.blockScheduleEnabled) {
                                val startStr = String.format("%02d:%02d", app.blockStartMinutes / 60, app.blockStartMinutes % 60)
                                val endStr = String.format("%02d:%02d", app.blockEndMinutes / 60, app.blockEndMinutes % 60)
                                Text("$startStr - $endStr", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.primary)
                            } else {
                                Text("Not scheduled", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary)
                            }
                        }
                    }

                    Switch(
                        checked = app.blockScheduleEnabled,
                        onCheckedChange = { onScheduleUpdate(it, app.blockStartMinutes, app.blockEndMinutes) },
                        modifier = Modifier.size(width = 40.dp, height = 24.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = LocalGKColors.current.primary,
                            checkedTrackColor = LocalGKColors.current.primary.copy(alpha = 0.25f),
                            uncheckedThumbColor = LocalGKColors.current.textTertiary,
                            uncheckedTrackColor = LocalGKColors.current.surface
                        )
                    )
                }

                if (app.blockScheduleEnabled) {
                    var showStartTimePicker by remember { mutableStateOf(false) }
                    var showEndTimePicker by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedButton(
                            onClick = { showStartTimePicker = true },
                            border = androidx.compose.foundation.BorderStroke(1.dp, LocalGKColors.current.border),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalGKColors.current.textPrimary)
                        ) {
                            val startStr = String.format("%02d:%02d", app.blockStartMinutes / 60, app.blockStartMinutes % 60)
                            Text("Start: $startStr", style = MaterialTheme.typography.bodySmall)
                        }

                        OutlinedButton(
                            onClick = { showEndTimePicker = true },
                            border = androidx.compose.foundation.BorderStroke(1.dp, LocalGKColors.current.border),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalGKColors.current.textPrimary)
                        ) {
                            val endStr = String.format("%02d:%02d", app.blockEndMinutes / 60, app.blockEndMinutes % 60)
                            Text("End: $endStr", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (showStartTimePicker) {
                        TimePickerDialog(
                            initialHour = app.blockStartMinutes / 60,
                            initialMinute = app.blockStartMinutes % 60,
                            onDismiss = { showStartTimePicker = false },
                            onConfirm = { h, m ->
                                onScheduleUpdate(true, h * 60 + m, app.blockEndMinutes)
                                showStartTimePicker = false
                            }
                        )
                    }

                    if (showEndTimePicker) {
                        TimePickerDialog(
                            initialHour = app.blockEndMinutes / 60,
                            initialMinute = app.blockEndMinutes % 60,
                            onDismiss = { showEndTimePicker = false },
                            onConfirm = { h, m ->
                                onScheduleUpdate(true, app.blockStartMinutes, h * 60 + m)
                                showEndTimePicker = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalGKColors.current.surfaceVariant,
        title = { Text("Select Time", color = LocalGKColors.current.textPrimary) },
        text = {
            TimePicker(state = state, colors = TimePickerDefaults.colors(
                clockDialColor = LocalGKColors.current.background,
                selectorColor = LocalGKColors.current.primary,
                containerColor = LocalGKColors.current.card,
                timeSelectorSelectedContainerColor = LocalGKColors.current.primary.copy(alpha = 0.2f),
                timeSelectorUnselectedContainerColor = LocalGKColors.current.card,
                timeSelectorSelectedContentColor = LocalGKColors.current.primary,
                timeSelectorUnselectedContentColor = LocalGKColors.current.textPrimary
            ))
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text("OK", color = LocalGKColors.current.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = LocalGKColors.current.textSecondary)
            }
        }
    )
}
