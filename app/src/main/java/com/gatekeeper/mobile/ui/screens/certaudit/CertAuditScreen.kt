package com.gatekeeper.mobile.ui.screens.certaudit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.components.SectionHeader
import com.gatekeeper.mobile.ui.components.ShimmerList
import com.gatekeeper.mobile.ui.components.GKBadge
import com.gatekeeper.mobile.ui.components.BadgeStyle
import com.gatekeeper.mobile.ui.theme.*
import com.gatekeeper.mobile.vpn.RogueCertInfo
import com.gatekeeper.mobile.vpn.VulnerableAppInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertAuditScreen(
    navController: NavController,
    viewModel: CertAuditViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val systemCerts by viewModel.systemCerts.collectAsState()
    val userCerts by viewModel.userCerts.collectAsState()
    val vulnerableApps by viewModel.vulnerableApps.collectAsState()
    val lastScanTime by viewModel.lastScanTime.collectAsState()

    val tabs = listOf("User (${userCerts.size})", "System (${systemCerts.size})")
    
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // Always rescan when screen opens so data is fresh
    LaunchedEffect(Unit) { viewModel.rescan() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Certificate Auditor", color = LocalGKColors.current.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LocalGKColors.current.textPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.injectDemoData() }) {
                        Icon(Icons.Default.Add, contentDescription = "Inject Demo Data", tint = LocalGKColors.current.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LocalGKColors.current.background)
            )
        },
        containerColor = LocalGKColors.current.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Hero
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(LocalGKColors.current.accentOrange.copy(alpha = 0.1f), LocalGKColors.current.background)))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "SSL Trust Store",
                        style = MaterialTheme.typography.displaySmall,
                        color = LocalGKColors.current.textPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    GKBadge("INCLUDES DEMO CA", BadgeStyle.MEDIUM)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "GateKeeper scans your device for Root Certificate Authorities. User CAs can be used to perform Man-in-the-Middle (MITM) attacks and decrypt your HTTPS traffic.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalGKColors.current.textSecondary
                    )
                }
            }

            if (isLoading) {
                ShimmerList(count = 5)
            } else {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = LocalGKColors.current.background,
                    contentColor = LocalGKColors.current.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                            text = { Text(title, color = if (pagerState.currentPage == index) LocalGKColors.current.primary else LocalGKColors.current.textSecondary) }
                        )
                    }
                }

                HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                    if (page == 0) {
                        // USER CERTS TAB
                        if (userCerts.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Default.Security,
                                title = "Trust Store Clean",
                                message = "No user-installed certificates found. You are safe from user-level MITM attacks.",
                                lastScanTime = lastScanTime,
                                onRescan = { viewModel.rescan() }
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (vulnerableApps.isNotEmpty()) {
                                    item {
                                        VulnerableAppsWarning(vulnerableApps)
                                        Spacer(Modifier.height(12.dp))
                                    }
                                }
                                item {
                                    SectionHeader("User-Installed Certificates")
                                    Spacer(Modifier.height(8.dp))
                                }
                                items(userCerts) { cert ->
                                    CertItemCard(cert) { viewModel.removeDemoData() }
                                }
                            }
                        }
                    } else {
                        // SYSTEM CERTS TAB
                        if (systemCerts.isEmpty()) {
                            EmptyStateView(
                                icon = Icons.Default.Info,
                                title = "No System Certificates",
                                message = "This is highly unusual and might indicate a damaged Android OS.",
                                lastScanTime = lastScanTime,
                                onRescan = { viewModel.rescan() }
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    Text("System certificates are baked into your Android OS and are implicitly trusted by almost all applications. They cannot be easily removed without root access.", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                                    Spacer(Modifier.height(16.dp))
                                }
                                items(systemCerts) { cert ->
                                    CertItemCard(cert) {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String, lastScanTime: Long, onRescan: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = LocalGKColors.current.accentGreen, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = LocalGKColors.current.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(message, color = LocalGKColors.current.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (lastScanTime > 0L) {
                Spacer(Modifier.height(8.dp))
                val elapsed = (System.currentTimeMillis() - lastScanTime) / 1000 / 60
                val label = if (elapsed < 1) "just now" else "${elapsed}m ago"
                Text("Last scanned: $label", color = LocalGKColors.current.textTertiary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onRescan,
                border = androidx.compose.foundation.BorderStroke(1.dp, LocalGKColors.current.primary)
            ) {
                Text("Rescan", color = LocalGKColors.current.primary)
            }
        }
    }
}

@Composable
fun VulnerableAppsWarning(vulnerableApps: List<VulnerableAppInfo>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LocalGKColors.current.accentRed.copy(alpha = 0.1f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, LocalGKColors.current.accentRed.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, null, tint = LocalGKColors.current.accentRed)
                Spacer(Modifier.width(8.dp))
                Text("Apps Vulnerable to User CAs", style = MaterialTheme.typography.titleMedium, color = LocalGKColors.current.accentRed, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text("The following installed apps target an older Android version (API < 24) and therefore natively trust your User Certificates. Their HTTPS traffic can be fully decrypted by whichever entity installed the User CA.", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textPrimary)
            Spacer(Modifier.height(12.dp))
            
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(LocalGKColors.current.card).padding(12.dp)) {
                vulnerableApps.forEachIndexed { index, app ->
                    Text(app.appName, style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.Medium)
                    Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textSecondary)
                    if (index < vulnerableApps.size - 1) {
                        HorizontalDivider(color = LocalGKColors.current.border, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CertItemCard(cert: RogueCertInfo, onRemoveDemo: () -> Unit) {
    val isHighRisk = cert.riskLevel == "HIGH"
    val color = if (isHighRisk) LocalGKColors.current.accentRed else if (cert.isUserInstalled) LocalGKColors.current.accentOrange else LocalGKColors.current.primary
    val icon = if (isHighRisk) Icons.Default.Warning else Icons.Default.Info

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LocalGKColors.current.card),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isHighRisk) "High Risk User Certificate" else if (cert.isUserInstalled) "User Certificate" else "System Certificate",
                        style = MaterialTheme.typography.titleMedium,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                    Text(cert.alias, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textTertiary, maxLines = 1)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = LocalGKColors.current.border)
            Spacer(Modifier.height(16.dp))
            
            Text("Issuer", style = MaterialTheme.typography.labelMedium, color = LocalGKColors.current.textTertiary)
            Text(cert.issuerName, style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textPrimary)
            
            Spacer(Modifier.height(8.dp))
            Text("Subject", style = MaterialTheme.typography.labelMedium, color = LocalGKColors.current.textTertiary)
            Text(cert.subjectName, style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textPrimary)
            
            Spacer(Modifier.height(8.dp))
            Text("Expires", style = MaterialTheme.typography.labelMedium, color = LocalGKColors.current.textTertiary)
            Text(cert.expiresAt, style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textPrimary)

            if (cert.isUserInstalled) {
                Spacer(Modifier.height(16.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        if (cert.alias == "user:demo_charles_proxy") {
                            onRemoveDemo()
                        } else {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color.copy(alpha = 0.15f),
                        contentColor = color
                    )
                ) {
                    Icon(Icons.Default.Warning, "Remove", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Remove from Device", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
