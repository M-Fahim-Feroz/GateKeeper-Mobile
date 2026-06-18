package com.gatekeeper.mobile.ui.screens.dns

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.data.db.entity.BlocklistSubscription
import com.gatekeeper.mobile.ui.components.GKInfoButton
import com.gatekeeper.mobile.ui.components.GKInfoDialog
import com.gatekeeper.mobile.ui.theme.*
import com.gatekeeper.mobile.vpn.GateKeeperVpnService

// ─── Main Screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DnsFilterScreen(
    navController: NavController,
    viewModel: DnsFilterViewModel = hiltViewModel()
) {
    val subscriptions by viewModel.subscriptions.collectAsState(initial = emptyList())
    val blacklistCount by viewModel.blacklistCount.collectAsState(initial = 0)
    val whitelistCount by viewModel.whitelistCount.collectAsState(initial = 0)
    val isSafeSearchEnabled by viewModel.isSafeSearchEnabled.collectAsState()
    val blacklist by viewModel.blacklist.collectAsState(initial = emptyList())
    val whitelist by viewModel.whitelist.collectAsState(initial = emptyList())
    val isVpnActive by GateKeeperVpnService.isRunning.collectAsState()

    // activeMode: "blacklist" = Block Mode (block the listed, allow rest)
    //             "whitelist" = Allow Mode (allow only listed, block rest)
    var activeMode by remember { mutableStateOf("blacklist") }

    var showAddFeedDialog by remember { mutableStateOf(false) }
    var showAddDomainDialog by remember { mutableStateOf(false) }
    var editingSubscription by remember { mutableStateOf<BlocklistSubscription?>(null) }

    val themeColor = Color(0xFF00D4FF)

    // Tabs: 0=Overview  1=Blocklist  2=Allowlist  3=Feeds
    var selectedTab by remember { mutableIntStateOf(0) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Overview", "Blocklist", "Allowlist", "Feeds")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalGKColors.current.background)
    ) {
        // TopAppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Security, contentDescription = null, tint = themeColor, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("GateKeeper", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = themeColor)
        }

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Web Gate", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textPrimary)
                Text(
                    if (isVpnActive) "DNS Protection Active" else "VPN Inactive — not filtering",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isVpnActive) LocalGKColors.current.accentGreen else Color(0xFFFFB300),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            GKInfoButton(color = themeColor) { showInfoDialog = true }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isVpnActive) themeColor.copy(alpha = 0.12f)
                        else Color(0xFFFFB300).copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isVpnActive) Icons.Filled.Security else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (isVpnActive) themeColor else Color(0xFFFFB300),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── Mode Toggle ─────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Block Mode pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (activeMode == "blacklist") LocalGKColors.current.accentRed.copy(alpha = 0.18f)
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (activeMode == "blacklist") LocalGKColors.current.accentRed.copy(alpha = 0.5f)
                        else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { activeMode = "blacklist" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Block, null,
                        tint = if (activeMode == "blacklist") LocalGKColors.current.accentRed
                               else LocalGKColors.current.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Block Mode",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (activeMode == "blacklist") FontWeight.Bold else FontWeight.Normal,
                        color = if (activeMode == "blacklist") LocalGKColors.current.accentRed
                                else LocalGKColors.current.textSecondary
                    )
                }
            }
            // Allow Mode pill
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (activeMode == "whitelist") LocalGKColors.current.accentGreen.copy(alpha = 0.18f)
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (activeMode == "whitelist") LocalGKColors.current.accentGreen.copy(alpha = 0.5f)
                        else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { activeMode = "whitelist" }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.CheckCircle, null,
                        tint = if (activeMode == "whitelist") LocalGKColors.current.accentGreen
                               else LocalGKColors.current.textSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Allow Mode",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (activeMode == "whitelist") FontWeight.Bold else FontWeight.Normal,
                        color = if (activeMode == "whitelist") LocalGKColors.current.accentGreen
                                else LocalGKColors.current.textSecondary
                    )
                }
            }
        }

        // Mode explanation banner
        val modeColor = if (activeMode == "blacklist") LocalGKColors.current.accentRed
                        else LocalGKColors.current.accentGreen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(modeColor.copy(alpha = 0.07f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (activeMode == "blacklist") Icons.Filled.Block else Icons.Filled.CheckCircle,
                null, tint = modeColor, modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (activeMode == "blacklist")
                    "Block Mode: All sites allowed except what's in your Blocklist."
                else
                    "Allow Mode: All sites blocked except what's in your Allowlist.",
                style = MaterialTheme.typography.labelSmall,
                color = LocalGKColors.current.textSecondary,
                lineHeight = 16.sp
            )
        }

        // ── Tab Row ──────────────────────────────────────────────────────
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = themeColor,
            edgePadding = 20.dp,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    Box(
                        modifier = Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(2.dp)
                            .padding(horizontal = 12.dp)
                            .background(themeColor, RoundedCornerShape(1.dp))
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, label ->
                val badge = when (index) {
                    1 -> blacklistCount
                    2 -> whitelistCount
                    3 -> subscriptions.size
                    else -> null
                }
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) themeColor
                                        else LocalGKColors.current.textSecondary
                            )
                            if (badge != null && badge > 0) {
                                Spacer(Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(themeColor.copy(alpha = if (selectedTab == index) 0.25f else 0.12f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        "$badge",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = themeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))

        // ── Tab Content ──────────────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> WebGateOverviewTab(
                    subscriptions = subscriptions,
                    blacklistCount = blacklistCount,
                    whitelistCount = whitelistCount,
                    isSafeSearchEnabled = isSafeSearchEnabled,
                    activeMode = activeMode,
                    themeColor = themeColor,
                    navController = navController,
                    viewModel = viewModel
                )
                1 -> WebGateDomainListTab(
                    title = "Blocklist",
                    description = "Sites added here will be blocked on this device.",
                    entries = blacklist,
                    color = LocalGKColors.current.accentRed,
                    emptyText = "No blocked domains yet. Tap + to add one.",
                    isActiveMode = activeMode == "blacklist",
                    activeModeLabel = "Block Mode active — this list is enforced",
                    onAdd = { showAddDomainDialog = true; activeMode = "blacklist" },
                    onRemove = { domain -> viewModel.removeDomain(domain, "blacklist") }
                )
                2 -> WebGateDomainListTab(
                    title = "Allowlist",
                    description = "Sites added here are always allowed, even if a feed blocks them.",
                    entries = whitelist,
                    color = LocalGKColors.current.accentGreen,
                    emptyText = "No allowed domains yet. Tap + to add one.",
                    isActiveMode = activeMode == "whitelist",
                    activeModeLabel = "Allow Mode active — only these sites work",
                    onAdd = { showAddDomainDialog = true; activeMode = "whitelist" },
                    onRemove = { domain -> viewModel.removeDomain(domain, "whitelist") }
                )
                3 -> WebGateFeedsTab(
                    subscriptions = subscriptions,
                    viewModel = viewModel,
                    themeColor = themeColor,
                    onAddFeed = { showAddFeedDialog = true },
                    onEditFeed = { editingSubscription = it }
                )
            }
        }
    }

    // ── Add Feed Dialog ───────────────────────────────────────────────────
    if (showAddFeedDialog || editingSubscription != null) {
        val dlgTheme = Color(0xFF00D4FF)
        val isEditing = editingSubscription != null
        var feedName by remember { mutableStateOf(editingSubscription?.name ?: "") }
        var feedUrl by remember { mutableStateOf(editingSubscription?.url ?: "") }
        var isError by remember { mutableStateOf(false) }
        val isValid = feedName.trim().isNotBlank() &&
            (feedUrl.trim().startsWith("http://") || feedUrl.trim().startsWith("https://"))

        AlertDialog(
            onDismissRequest = { showAddFeedDialog = false; editingSubscription = null },
            title = { Text(if (isEditing) "Edit Filter List" else "Add Filter List") },
            text = {
                Column {
                    OutlinedTextField(
                        value = feedName, onValueChange = { feedName = it },
                        label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = dlgTheme,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedLabelColor = dlgTheme,
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
                            focusedBorderColor = dlgTheme,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedLabelColor = dlgTheme,
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
                            if (isEditing) viewModel.updateSubscription(editingSubscription!!, feedName.trim(), feedUrl.trim())
                            else viewModel.addSubscription(feedName.trim(), feedUrl.trim())
                            showAddFeedDialog = false; editingSubscription = null
                        } else { isError = true }
                    },
                    enabled = isValid
                ) { Text(if (isEditing) "Save" else "Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddFeedDialog = false; editingSubscription = null }) { Text("Cancel") }
            }
        )
    }

    // ── Add Domain Dialog ─────────────────────────────────────────────────
    if (showAddDomainDialog) {
        val dlgTheme = Color(0xFF00D4FF)
        var domainName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDomainDialog = false },
            title = { Text("Add to ${if (activeMode == "blacklist") "Blocklist" else "Allowlist"}") },
            text = {
                Column {
                    Text(
                        if (activeMode == "blacklist")
                            "This site will be blocked on the device."
                        else
                            "This site will always be allowed, even if a feed blocks it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalGKColors.current.textSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = domainName,
                        onValueChange = { domainName = it },
                        label = { Text("Domain (e.g., example.com)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = dlgTheme,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            focusedLabelColor = dlgTheme,
                            unfocusedLabelColor = LocalGKColors.current.textSecondary,
                            focusedTextColor = LocalGKColors.current.textPrimary,
                            unfocusedTextColor = LocalGKColors.current.textPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (domainName.isNotBlank()) {
                        viewModel.addDomain(domainName.trim(), activeMode)
                        showAddDomainDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDomainDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showInfoDialog) {
        GKInfoDialog(
            title = "Web Gate",
            body = "Web Gate filters websites before they even load on your device.\n\nIt works like a bouncer at the door — dangerous sites, ads, and trackers are blocked at the network level so they never reach your browser or apps.\n\n• Block Mode: All websites work normally except the ones you've added to your Blocklist.\n• Allow Mode: Only websites on your Allowlist can load — everything else is blocked.",
            accentColor = themeColor,
            onDismiss = { showInfoDialog = false }
        )
    }
}

// ─── Tab 0: Overview ────────────────────────────────────────────────────────

@Composable
fun WebGateOverviewTab(
    subscriptions: List<BlocklistSubscription>,
    blacklistCount: Int,
    whitelistCount: Int,
    isSafeSearchEnabled: Boolean,
    activeMode: String,
    themeColor: Color,
    navController: NavController,
    viewModel: DnsFilterViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatPill("Filter\nFeeds", subscriptions.size.toString(), themeColor, Modifier.weight(1f))
                StatPill("Blocked\nDomains", blacklistCount.toString(), LocalGKColors.current.accentRed, Modifier.weight(1f))
                StatPill("Allowed\nDomains", whitelistCount.toString(), LocalGKColors.current.accentGreen, Modifier.weight(1f))
            }
        }

        // Quick Filters
        item {
            Text(
                "Quick Filters",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = LocalGKColors.current.textPrimary
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(110.dp)) {
                BentoFilterCard(
                    title = "Safe Search",
                    subtitle = if (isSafeSearchEnabled) "Active" else "Disabled",
                    icon = Icons.Filled.Block,
                    isChecked = isSafeSearchEnabled,
                    color = themeColor,
                    onCheckedChange = { viewModel.setSafeSearchEnabled(it) },
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .clickable { navController.navigate(com.gatekeeper.mobile.ui.navigation.Screen.ThreatFeed.route) }
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF37DF66).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.Security, null, tint = Color(0xFF37DF66)) }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(16.dp))
                        }
                        Column {
                            Text("Threat Feeds", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = LocalGKColors.current.textPrimary)
                            Text("Manage lists", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                        }
                    }
                }
            }
        }

        // Recent active feeds summary
        if (subscriptions.isNotEmpty()) {
            item {
                Text(
                    "Active Filter Lists (${subscriptions.count { it.isEnabled }})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalGKColors.current.textPrimary
                )
            }
            items(subscriptions.filter { it.isEnabled }.take(3)) { sub ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(themeColor.copy(alpha = 0.1f))
                                .border(1.dp, themeColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Security, null, tint = themeColor, modifier = Modifier.size(20.dp)) }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(sub.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = LocalGKColors.current.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${sub.domainCount} rules", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(LocalGKColors.current.accentGreen.copy(alpha = 0.1f))
                            .border(1.dp, LocalGKColors.current.accentGreen.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = LocalGKColors.current.accentGreen, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.accentGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textSecondary, textAlign = TextAlign.Center)
        }
    }
}

// ─── Tab 1 & 2: Domain List Tab (reused for Blocklist and Allowlist) ─────────

@Composable
fun WebGateDomainListTab(
    title: String,
    description: String,
    entries: List<com.gatekeeper.mobile.data.db.entity.DnsEntry>,
    color: Color,
    emptyText: String,
    isActiveMode: Boolean,
    activeModeLabel: String,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header + Add button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(description, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                }
                FilledTonalButton(
                    onClick = onAdd,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = color.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, null, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add", color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active mode badge
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActiveMode) color.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f))
                    .border(1.dp, if (isActiveMode) color.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isActiveMode) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    null,
                    tint = if (isActiveMode) color else LocalGKColors.current.textTertiary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isActiveMode) activeModeLabel else "This list is not currently enforced. Switch mode to activate.",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isActiveMode) color else LocalGKColors.current.textTertiary
                )
            }
        }

        // Divider + count
        if (entries.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${entries.size} entries", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary)
                    Spacer(Modifier.width(8.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.05f))
                }
            }
        }

        // List or empty state
        if (entries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Inbox, null, tint = LocalGKColors.current.textTertiary, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(emptyText, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textTertiary, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(entries, key = { it.domain }) { entry ->
                DnsEntryCard(
                    entry = entry,
                    color = color,
                    onRemove = { onRemove(entry.domain) }
                )
            }
        }
    }
}

// ─── Tab 3: Feeds ────────────────────────────────────────────────────────────

@Composable
fun WebGateFeedsTab(
    subscriptions: List<BlocklistSubscription>,
    viewModel: DnsFilterViewModel,
    themeColor: Color,
    onAddFeed: () -> Unit,
    onEditFeed: (BlocklistSubscription) -> Unit
) {
    val availableFeeds = viewModel.recommendedFeeds.filter { feed ->
        subscriptions.none { it.url == feed.url }
    }
    var showFeedsInfo by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Filter Lists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textPrimary)
                        GKInfoButton(color = themeColor, modifier = Modifier.padding(start = 8.dp)) { showFeedsInfo = true }
                    }
                    Text("Auto-updated blocklists from trusted security sources.", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                }
                FilledTonalButton(
                    onClick = onAddFeed,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = themeColor.copy(alpha = 0.15f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.Add, null, tint = themeColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Feed", color = themeColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active subscriptions
        if (subscriptions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Inbox, null, tint = LocalGKColors.current.textTertiary, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("No filter lists added yet.", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textTertiary)
                        Text("Add one above or pick from recommendations below.", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            item {
                Text(
                    "Your Lists (${subscriptions.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalGKColors.current.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(subscriptions, key = { it.url }) { sub ->
                FilterListCard(subscription = sub, color = themeColor, viewModel = viewModel, onEdit = { onEditFeed(sub) })
            }
        }

        // Recommended feeds
        if (availableFeeds.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Recommended Feeds",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalGKColors.current.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(availableFeeds, key = { it.url }) { feed ->
                RecommendedDnsFeedCard(feed = feed, color = themeColor, onAdd = { viewModel.importFeed(feed) })
            }
        }
    }

    if (showFeedsInfo) {
        GKInfoDialog(
            title = "Filter Lists",
            body = "Filter lists are community-maintained databases containing millions of known bad websites — ads, trackers, malware, and phishing pages.\n\nEnabling one instantly protects you from every domain on that list, without you needing to add anything manually.\n\nThey update automatically so you're always protected against new threats.",
            accentColor = themeColor,
            onDismiss = { showFeedsInfo = false }
        )
    }
}

// ─── Shared Card Composables ──────────────────────────────────────────────────

@Composable
fun BentoFilterCard(
    title: String, subtitle: String, icon: ImageVector, isChecked: Boolean, color: Color,
    onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!isChecked) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (isChecked) Color(0xFF5203D5).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
                        .border(1.dp, if (isChecked) color.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = if (isChecked) color else LocalGKColors.current.textSecondary)
                }
                Box(
                    modifier = Modifier.width(44.dp).height(24.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (isChecked) Color(0xFF5203D5).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f))
                        .border(1.dp, if (isChecked) color else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = if (isChecked) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(modifier = Modifier.padding(2.dp).size(18.dp).clip(CircleShape).background(if (isChecked) color else LocalGKColors.current.textSecondary))
                }
            }
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = LocalGKColors.current.textPrimary)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.Info, null, tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = if (isChecked) LocalGKColors.current.textSecondary else LocalGKColors.current.textTertiary)
            }
        }
    }
}

@Composable
fun FilterListCard(subscription: BlocklistSubscription, color: Color, viewModel: DnsFilterViewModel, onEdit: () -> Unit) {
    val dateFormatter = remember { java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()) }
    val lastUpdatedText = if (subscription.lastRefreshedAt > 0) dateFormatter.format(java.util.Date(subscription.lastRefreshedAt)) else "Never"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable { onEdit() }
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.1f))
                        .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Security, null, tint = color) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(subscription.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = LocalGKColors.current.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subscription.url, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Switch(
                checked = subscription.isEnabled,
                onCheckedChange = { viewModel.toggleSubscription(subscription, it) },
                colors = SwitchDefaults.colors(checkedThumbColor = color, checkedTrackColor = color.copy(alpha = 0.3f))
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.2f))
                        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                ) { Text("${subscription.domainCount} Rules", style = MaterialTheme.typography.labelSmall, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(8.dp))
                val statusColor = when (subscription.fetchStatus) {
                    "SUCCESS" -> LocalGKColors.current.accentGreen
                    "FAILED" -> LocalGKColors.current.accentRed
                    "FETCHING" -> LocalGKColors.current.accentYellow
                    else -> LocalGKColors.current.textTertiary
                }
                Text(
                    if (subscription.fetchStatus == "FAILED") "Failed: ${subscription.errorReason ?: "Unknown"}" else "Sync: $lastUpdatedText",
                    style = MaterialTheme.typography.labelSmall, color = statusColor, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Row {
                IconButton(onClick = { viewModel.toggleSubscription(subscription, true) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Refresh, null, tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { viewModel.deleteSubscription(subscription) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.DeleteOutline, null, tint = LocalGKColors.current.accentRed, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun ActionButton(title: String, icon: ImageVector, iconTint: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = LocalGKColors.current.textPrimary)
    }
}

@Composable
fun RecommendedDnsFeedCard(feed: DnsFilterViewModel.FeedSource, color: Color, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(feed.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = LocalGKColors.current.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(feed.description, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(feed.url, style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        IconButton(onClick = onAdd, modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(alpha = 0.2f))) {
            Icon(Icons.Filled.Add, contentDescription = "Add", tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun DnsEntryCard(entry: com.gatekeeper.mobile.data.db.entity.DnsEntry, color: Color, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (color == LocalGKColors.current.accentGreen) Icons.Filled.CheckCircle else Icons.Filled.Block,
                    null, tint = color, modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(entry.domain, style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(18.dp))
        }
    }
}
