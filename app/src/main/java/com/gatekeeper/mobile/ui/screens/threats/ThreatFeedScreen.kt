package com.gatekeeper.mobile.ui.screens.threats

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gatekeeper.mobile.ui.components.*
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.theme.*

@Composable
fun ThreatFeedScreen(
    navController: NavController? = null,
    viewModel: ThreatFeedViewModel = hiltViewModel()
) {
    val allThreats by viewModel.allThreats.collectAsState(initial = emptyList())
    val isLoading by viewModel.isLoading.collectAsState(initial = false)
    val importStatus by viewModel.importStatus.collectAsState(initial = null)
    val totalThreats = allThreats.size

    var customFeedUrl by remember { mutableStateOf("") }

    // Group imported feeds by name
    val importedFeedNames = remember(allThreats) { allThreats.map { it.feedName }.toSet() }

    // Auto-clear status after 3 seconds for non-loading states
    LaunchedEffect(importStatus) {
        importStatus?.let {
            if (it.contains("Successfully") || it.contains("Failed") || it.contains("removed")) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearStatus()
            }
        }
    }

    Scaffold(
        containerColor = LocalGKColors.current.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalGKColors.current.background)
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // ── Header ──
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(LocalGKColors.current.accentRed.copy(alpha = 0.08f), LocalGKColors.current.background)))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Back button for when navigated from Dashboard
                    if (navController != null) {
                        IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.ArrowBack, "Back", tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(GradientDanger.map { it.copy(alpha = 0.2f) })),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Security, null, tint = LocalGKColors.current.accentRed, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Threat Intelligence", style = MaterialTheme.typography.displaySmall, color = LocalGKColors.current.textPrimary)
                            Text(
                                if (totalThreats > 0) "$totalThreats malicious indicators active" else "No feeds imported yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalGKColors.current.textSecondary
                            )
                        }
                    }

                    // Stats
                    if (totalThreats > 0) {
                        Spacer(Modifier.height(16.dp))
                        val ipCount = allThreats.count { it.indicatorType == "ip" }
                        val domainCount = allThreats.count { it.indicatorType == "domain" }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard("IP Threats", "$ipCount", GradientDanger, Modifier.weight(1f))
                            StatCard("Domain Threats", "$domainCount", GradientPurple, Modifier.weight(1f))
                        }
                    }

                    // Clear all button
                    if (totalThreats > 0) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { viewModel.clearAll() },
                            enabled = !isLoading,
                            colors = ButtonDefaults.textButtonColors(contentColor = LocalGKColors.current.accentRed)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clear All Feeds", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Active Feeds Section ──
            if (importedFeedNames.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        SectionHeader(title = "Active Feeds (${importedFeedNames.size})")
                        Spacer(Modifier.height(8.dp))
                    }
                }
                
                val grouped = allThreats.groupBy { it.feedName }
                items(grouped.entries.toList()) { (feedName, entries) ->
                    val sourceUrl = entries.first().feedSource
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        GKListRow(
                            icon = Icons.Filled.CheckCircle, iconTint = LocalGKColors.current.accentGreen,
                            title = feedName,
                            subtitle = "${entries.size} indicators",
                            trailing = {
                                IconButton(onClick = { viewModel.removeFeed(sourceUrl) }) {
                                    Icon(Icons.Filled.DeleteOutline, "Remove Feed", tint = LocalGKColors.current.accentRed)
                                }
                            }
                        )
                    }
                }
                
                item { Spacer(Modifier.height(16.dp)) }
            }

            // ── Loading indicator ──
            if (isLoading) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = LocalGKColors.current.primary, strokeWidth = 2.dp)
                            Spacer(Modifier.height(12.dp))
                            Text("Downloading & processing feed...", color = LocalGKColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
                            Text("Large feeds may take 10–30 seconds.", color = LocalGKColors.current.textTertiary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else if (importStatus != null) {
                item {
                    androidx.compose.animation.AnimatedContent(targetState = importStatus, label = "import_status") { status ->
                        val isError = status?.contains("Failed") == true
                        val isSuccess = status?.contains("Successfully") == true || status?.contains("removed") == true
                        val color = when {
                            isError -> LocalGKColors.current.accentRed
                            isSuccess -> LocalGKColors.current.accentGreen
                            else -> LocalGKColors.current.primary
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(color.copy(alpha = 0.15f))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(status ?: "", color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Custom URL import ──
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    SectionHeader(title = "Custom Feed URL")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customFeedUrl,
                            onValueChange = { customFeedUrl = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("https://example.com/blocklist.txt", color = LocalGKColors.current.textTertiary) },
                            shape = RoundedCornerShape(12.dp),
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
                        Spacer(Modifier.width(12.dp))
                        FilledIconButton(
                            onClick = {
                                if (customFeedUrl.isNotBlank() && !isLoading) {
                                    val safeUrl = if (!customFeedUrl.startsWith("http://") && !customFeedUrl.startsWith("https://")) {
                                        "https://" + customFeedUrl.trim()
                                    } else {
                                        customFeedUrl.trim()
                                    }
                                    
                                    val customFeed = ThreatFeedViewModel.FeedSource(
                                        name = safeUrl.substringAfterLast("/").ifBlank { "Custom Feed" },
                                        url = safeUrl,
                                        type = "domain",
                                        threatType = "custom",
                                        description = "Manually imported feed"
                                    )
                                    viewModel.importFeed(customFeed)
                                    customFeedUrl = ""
                                }
                            },
                            enabled = !isLoading && customFeedUrl.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(54.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = LocalGKColors.current.primary,
                                contentColor = LocalGKColors.current.background,
                                disabledContainerColor = LocalGKColors.current.surfaceVariant,
                                disabledContentColor = LocalGKColors.current.textTertiary
                            )
                        ) {
                            Icon(Icons.Filled.Download, null)
                        }
                    }
                }
            }

            // ── Curated Feeds Section ──
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SectionHeader(title = "Curated Threat Feeds")
                        TextButton(
                            onClick = { viewModel.importAllRecommended() },
                            enabled = !isLoading,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Import All", color = LocalGKColors.current.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Tap any feed to import it. All feeds are free and updated regularly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalGKColors.current.textTertiary
                    )
                }
            }

            items(viewModel.recommendedFeeds) { feed ->
                val isImported = importedFeedNames.contains(feed.name)
                ThreatFeedCard(
                    feed = feed,
                    isImported = isImported,
                    isLoading = isLoading,
                    onImport = { viewModel.importFeed(feed) },
                    onRemove = { viewModel.removeFeed(feed.url) }
                )
            }

        }
    }
}

@Composable
fun ThreatFeedCard(
    feed: ThreatFeedViewModel.FeedSource,
    isImported: Boolean,
    isLoading: Boolean,
    onImport: () -> Unit,
    onRemove: () -> Unit
) {
    val borderColor = if (isImported) LocalGKColors.current.accentGreen.copy(alpha = 0.4f) else LocalGKColors.current.border
    val bgColor = if (isImported) LocalGKColors.current.accentGreen.copy(alpha = 0.05f) else LocalGKColors.current.card

    val (typeIcon, typeColor) = when (feed.threatType) {
        "malware-c2" -> Icons.Filled.BugReport to LocalGKColors.current.accentRed
        "malware"    -> Icons.Filled.Warning to LocalGKColors.current.accentRed
        "phishing"   -> Icons.Filled.Report to LocalGKColors.current.accentOrange
        "ads-tracking" -> Icons.Filled.Block to SecondaryPurple
        "cryptominer" -> Icons.Filled.ElectricBolt to LocalGKColors.current.accentYellow
        "ransomware" -> Icons.Filled.Lock to LocalGKColors.current.accentRed
        else          -> Icons.Filled.Security to LocalGKColors.current.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .then(
                Modifier.background(
                    Brush.horizontalGradient(listOf(borderColor.copy(alpha = 0f), borderColor.copy(alpha = 0f)))
                )
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Type icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(typeColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(typeIcon, null, tint = typeColor, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    feed.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalGKColors.current.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                if (isImported) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.CheckCircle, null, tint = LocalGKColors.current.accentGreen, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                feed.description,
                style = MaterialTheme.typography.bodySmall,
                color = LocalGKColors.current.textTertiary,
                maxLines = 2
            )
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(typeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "${feed.type.uppercase()} • ${feed.threatType.replace("-", " ").uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = typeColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Action button
        if (isImported) {
            IconButton(
                onClick = onRemove,
                enabled = !isLoading,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Filled.DeleteOutline, "Remove", tint = LocalGKColors.current.accentRed, modifier = Modifier.size(18.dp))
            }
        } else {
            FilledTonalButton(
                onClick = onImport,
                enabled = !isLoading,
                modifier = Modifier.height(34.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = LocalGKColors.current.accentRed.copy(alpha = 0.15f),
                    contentColor = LocalGKColors.current.accentRed
                )
            ) {
                Icon(Icons.Filled.Download, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Import", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
