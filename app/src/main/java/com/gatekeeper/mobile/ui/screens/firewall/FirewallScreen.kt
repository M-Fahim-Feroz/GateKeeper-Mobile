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
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        // Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(PrimaryCyan.copy(alpha = 0.06f), DarkBackground)))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(GradientPrimary.map { it.copy(alpha = 0.2f) })),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Shield, null, tint = PrimaryCyan, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("App Firewall", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                    Text("$excludedCount apps blocked from internet", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            // VPN warning banner
            if (!isVpnActive) {
                Box(Modifier.fillMaxWidth().height(3.dp).background(AccentOrange.copy(alpha = 0.7f)))
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search installed apps...", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = TextTertiary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, null, tint = TextTertiary)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
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
                        .background(AccentOrange.copy(alpha = 0.1f))
                        .padding(16.dp)
                ) {
                    Row {
                        Icon(Icons.Filled.WarningAmber, null, tint = AccentOrange, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Global DNS Filter Paused", style = MaterialTheme.typography.titleSmall, color = AccentOrange, fontWeight = FontWeight.Bold)
                            Text(
                                "Due to Android OS limitations, when the App Firewall is active, all unblocked apps must bypass the VPN entirely to maintain internet speed. The DNS Filter will only resume when all apps are unblocked.",
                                style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(top = 4.dp)
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
                        selectedContainerColor = PrimaryCyan.copy(alpha = 0.15f),
                        selectedLabelColor = PrimaryCyan
                    )
                )
                FilterChip(
                    selected = showExcludedOnly,
                    onClick = { showExcludedOnly = true; showAllowedOnly = false },
                    label = { Text("Blocked ($excludedCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentRed.copy(alpha = 0.15f),
                        selectedLabelColor = AccentRed
                    )
                )
                FilterChip(
                    selected = showAllowedOnly,
                    onClick = { showAllowedOnly = true; showExcludedOnly = false },
                    label = { Text("Allowed (${apps.size - excludedCount})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentGreen.copy(alpha = 0.15f),
                        selectedLabelColor = AccentGreen
                    )
                )
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryCyan, strokeWidth = 2.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("Loading installed apps...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
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
                        onScreenOffToggle = { block -> viewModel.toggleScreenOffBlock(app.packageName, app.appName, block) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun AppFirewallItem(
    app: InstalledApp,
    isScreenOffBlocked: Boolean,
    onToggle: (Boolean) -> Unit,
    onScreenOffToggle: (Boolean) -> Unit
) {
    val excluded = app.isBlocked
    val shape = RoundedCornerShape(14.dp)
    val borderBrush = if (excluded)
        Brush.horizontalGradient(GradientOrange.map { it.copy(alpha = 0.4f) })
    else
        Brush.horizontalGradient(listOf(GlassBorder, GlassBorder))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DarkCard)
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
                        .background(if (excluded) AccentOrange.copy(alpha = 0.12f) else PrimaryCyan.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        coil.compose.AsyncImage(model = appIcon, contentDescription = app.appName, modifier = Modifier.size(28.dp))
                    } else {
                        Icon(Icons.Filled.Apps, null, tint = if (excluded) AccentOrange else PrimaryCyan, modifier = Modifier.size(22.dp))
                    }
                }
                // Risk dot overlay
                if (app.sensitivePermCount >= 3) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(AccentRed))
                } else if (app.sensitivePermCount >= 1) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(AccentOrange))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = TextTertiary, maxLines = 1)
            }

            // Internet Block toggle
            Switch(
                checked = excluded,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentRed,
                    checkedTrackColor = AccentRed.copy(alpha = 0.25f),
                    uncheckedThumbColor = AccentGreen.copy(alpha = 0.8f),
                    uncheckedTrackColor = AccentGreen.copy(alpha = 0.15f)
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
                        tint = if (isScreenOffBlocked) AccentOrange else TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Block when screen off",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isScreenOffBlocked) AccentOrange else TextTertiary
                    )
                }
                Switch(
                    checked = isScreenOffBlocked,
                    onCheckedChange = { onScreenOffToggle(it) },
                    modifier = Modifier.size(width = 40.dp, height = 24.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AccentOrange,
                        checkedTrackColor = AccentOrange.copy(alpha = 0.25f),
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = DarkSurface
                    )
                )
            }
        }
    }
}
