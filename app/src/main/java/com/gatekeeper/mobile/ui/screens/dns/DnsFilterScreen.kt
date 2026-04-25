package com.gatekeeper.mobile.ui.screens.dns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.gatekeeper.mobile.data.db.entity.DnsEntry
import com.gatekeeper.mobile.ui.components.*
import com.gatekeeper.mobile.ui.theme.*
import com.gatekeeper.mobile.vpn.GateKeeperVpnService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsFilterScreen(viewModel: DnsFilterViewModel = hiltViewModel()) {
    var isBlacklistMode by remember { mutableStateOf(true) }
    var newDomain by remember { mutableStateOf("") }
    var domainError by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val blacklist by viewModel.blacklist.collectAsState(initial = emptyList())
    val whitelist by viewModel.whitelist.collectAsState(initial = emptyList())
    val blacklistCount by viewModel.blacklistCount.collectAsState(initial = 0)
    val whitelistCount by viewModel.whitelistCount.collectAsState(initial = 0)
    val isVpnActive by GateKeeperVpnService.isRunning.collectAsState()

    val currentList = if (isBlacklistMode) blacklist else whitelist
    val currentListType = if (isBlacklistMode) "blacklist" else "whitelist"

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Domain validation logic
    fun validateAndCleanDomain(raw: String): Pair<String?, String?> {
        var cleaned = raw.trim().lowercase()
        // Strip protocol prefixes
        cleaned = cleaned.removePrefix("https://").removePrefix("http://").removePrefix("www.")
        // Strip path
        cleaned = cleaned.substringBefore("/")
        // Validate: must contain a dot, no spaces, no special chars
        if (cleaned.isEmpty()) return null to "Domain cannot be empty"
        if (cleaned.contains(" ")) return null to "Domain cannot contain spaces"
        if (!cleaned.contains(".")) return null to "Enter a valid domain (e.g. youtube.com)"
        if (cleaned.length > 253) return null to "Domain is too long"
        val validPattern = Regex("^[a-z0-9.-]+$")
        if (!validPattern.matches(cleaned)) return null to "Domain contains invalid characters"
        return cleaned to null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues)
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(SecondaryPurple.copy(alpha = 0.07f), DarkBackground)))
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(GradientPurple.map { it.copy(alpha = 0.2f) })),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Dns, null, tint = SecondaryPurple, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("DNS Filter", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                        Text("Block & allow domains at DNS level", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Spacer(Modifier.weight(1f))
                    FloatingActionButton(
                        onClick = { showAddDialog = true; newDomain = ""; domainError = null },
                        modifier = Modifier.size(44.dp),
                        containerColor = SecondaryPurple.copy(alpha = 0.2f),
                        contentColor = SecondaryPurple
                    ) {
                        Icon(Icons.Filled.Add, "Add domain", modifier = Modifier.size(20.dp))
                    }
                }

                // VPN warning banner
                if (!isVpnActive) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentOrange.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, null, tint = AccentOrange, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "VPN is OFF — DNS blocking is not active",
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Stats row
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Blocklisted", "$blacklistCount", GradientDanger, Modifier.weight(1f))
                    StatCard("Allowlisted", "$whitelistCount", GradientSuccess, Modifier.weight(1f))
                }

                Spacer(Modifier.height(14.dp))

                // Mode toggle chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isBlacklistMode,
                        onClick = { isBlacklistMode = true },
                        label = { Text("Blocklist") },
                        leadingIcon = { Icon(Icons.Filled.Block, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentRed.copy(alpha = 0.15f),
                            selectedLabelColor = AccentRed
                        )
                    )
                    FilterChip(
                        selected = !isBlacklistMode,
                        onClick = { isBlacklistMode = false },
                        label = { Text("Allowlist") },
                        leadingIcon = { Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGreen.copy(alpha = 0.15f),
                            selectedLabelColor = AccentGreen
                        )
                    )
                }
            }

            if (currentList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.FilterListOff, null, tint = TextTertiary, modifier = Modifier.size(52.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (isBlacklistMode) "No domains blocked yet" else "No domains allowed yet",
                            color = TextSecondary, style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Tap + to add one", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(currentList, key = { it.id }) { entry ->
                        DnsEntryItem(
                            entry = entry,
                            isBlacklisted = isBlacklistMode,
                            onDelete = { viewModel.removeDomain(entry.domain, currentListType) }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; newDomain = ""; domainError = null },
            containerColor = DarkSurfaceVariant,
            title = { Text(if (isBlacklistMode) "Block Domain" else "Allow Domain", color = TextPrimary) },
            text = {
                Column {
                    Text(
                        "Enter a domain name. The http:// and www. will be removed automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newDomain,
                        onValueChange = { newDomain = it; domainError = null },
                        label = { Text("e.g. youtube.com or https://youtube.com", color = TextTertiary) },
                        isError = domainError != null,
                        supportingText = {
                            if (domainError != null) {
                                Text(domainError!!, color = AccentRed, style = MaterialTheme.typography.bodySmall)
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (domainError != null) AccentRed else PrimaryCyan,
                            unfocusedBorderColor = if (domainError != null) AccentRed else GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val (cleaned, error) = validateAndCleanDomain(newDomain)
                    if (error != null) {
                        domainError = error
                    } else if (cleaned != null) {
                        viewModel.addDomain(cleaned, currentListType)
                        showAddDialog = false
                        newDomain = ""
                        domainError = null
                        val action = if (isBlacklistMode) "blocked" else "allowed"
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "✓  $cleaned  $action successfully",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }) { Text("Add", color = PrimaryCyan, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; newDomain = ""; domainError = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun DnsEntryItem(entry: DnsEntry, isBlacklisted: Boolean, onDelete: () -> Unit) {
    val accentColor = if (isBlacklisted) AccentRed else AccentGreen
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isBlacklisted) Icons.Filled.Block else Icons.Filled.CheckCircle,
            null, tint = accentColor, modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.domain,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                fontWeight = FontWeight.Medium
            )
            Text(
                if (entry.source == "user") "Added manually" else "Source: ${entry.source}",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                maxLines = 1
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.DeleteOutline, "Delete", tint = TextTertiary, modifier = Modifier.size(18.dp))
        }
    }
}
