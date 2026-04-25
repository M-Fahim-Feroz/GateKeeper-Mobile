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

    val snackbarHostState = remember { SnackbarHostState() }

    // Show import status as snackbar
    LaunchedEffect(importStatus) {
        importStatus?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // ── Header ──
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(AccentRed.copy(alpha = 0.08f), DarkBackground)))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(GradientDanger.map { it.copy(alpha = 0.2f) })),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Security, null, tint = AccentRed, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Threat Intelligence", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                            Text(
                                if (totalThreats > 0) "$totalThreats malicious indicators active" else "No feeds imported yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
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
                            colors = ButtonDefaults.textButtonColors(contentColor = AccentRed)
                        ) {
                            Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clear All Feeds", fontWeight = FontWeight.Bold)
                        }
                    }
                }
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
                            CircularProgressIndicator(color = PrimaryCyan, strokeWidth = 2.dp)
                            Spacer(Modifier.height(12.dp))
                            Text("Downloading & processing feed...", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                            Text("Large feeds may take 10–30 seconds.", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
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
                            placeholder = { Text("https://example.com/blocklist.txt", color = TextTertiary) },
                            shape = RoundedCornerShape(12.dp),
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
                        Spacer(Modifier.width(12.dp))
                        FilledIconButton(
                            onClick = {
                                if (customFeedUrl.isNotBlank() && !isLoading) {
                                    val customFeed = ThreatFeedViewModel.FeedSource(
                                        name = customFeedUrl.substringAfterLast("/").ifBlank { "Custom Feed" },
                                        url = customFeedUrl.trim(),
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
                                containerColor = PrimaryCyan,
                                contentColor = DarkBackground,
                                disabledContainerColor = DarkSurfaceVariant,
                                disabledContentColor = TextTertiary
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
                    SectionHeader(title = "Curated Threat Feeds")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Tap any feed to import it. All feeds are free and updated regularly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
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

            // ── Imported Threats List ──
            if (allThreats.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                        SectionHeader(title = "Active Indicators")
                    }
                }

                val grouped = allThreats.groupBy { it.feedName }
                grouped.forEach { (feedName, entries) ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PrimaryCyan.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(feedName, style = MaterialTheme.typography.labelMedium, color = PrimaryCyan, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("${entries.size} indicators", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        }
                    }
                    items(entries.take(30)) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkCard)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (icon, tint) = when (entry.indicatorType) {
                                "ip" -> Icons.Filled.Router to AccentOrange
                                else -> Icons.Filled.Language to AccentRed
                            }
                            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(entry.indicator, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 1)
                                Text(entry.threatType.uppercase(), style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                            }
                        }
                    }
                    if (entries.size > 30) {
                        item {
                            Text(
                                "  + ${entries.size - 30} more entries",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextTertiary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
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
    val borderColor = if (isImported) AccentGreen.copy(alpha = 0.4f) else GlassBorder
    val bgColor = if (isImported) AccentGreen.copy(alpha = 0.05f) else DarkCard

    val (typeIcon, typeColor) = when (feed.threatType) {
        "malware-c2" -> Icons.Filled.BugReport to AccentRed
        "malware"    -> Icons.Filled.Warning to AccentRed
        "phishing"   -> Icons.Filled.Report to AccentOrange
        "ads-tracking" -> Icons.Filled.Block to SecondaryPurple
        "cryptominer" -> Icons.Filled.ElectricBolt to AccentYellow
        "ransomware" -> Icons.Filled.Lock to AccentRed
        else          -> Icons.Filled.Security to PrimaryCyan
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
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                if (isImported) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                feed.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
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
                Icon(Icons.Filled.DeleteOutline, "Remove", tint = AccentRed, modifier = Modifier.size(18.dp))
            }
        } else {
            FilledTonalButton(
                onClick = onImport,
                enabled = !isLoading,
                modifier = Modifier.height(34.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AccentRed.copy(alpha = 0.15f),
                    contentColor = AccentRed
                )
            ) {
                Icon(Icons.Filled.Download, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Import", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
