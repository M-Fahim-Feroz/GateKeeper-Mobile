package com.gatekeeper.mobile.ui.screens.certaudit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.components.GKInfoButton
import com.gatekeeper.mobile.ui.components.GKInfoDialog
import com.gatekeeper.mobile.ui.theme.*
import com.gatekeeper.mobile.vpn.CertificateInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertAuditScreen(
    navController: NavController? = null,
    viewModel: CertAuditViewModel = hiltViewModel()
) {
    val userCerts by viewModel.userCerts.collectAsState()
    val systemCerts by viewModel.systemCerts.collectAsState()
    val vulnerableApps by viewModel.vulnerableApps.collectAsState()

    LaunchedEffect(Unit) { viewModel.rescan() }

    var selectedCert by remember { mutableStateOf<CertificateInfo?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val trustScore = 100 - (userCerts.size * 10) - (vulnerableApps.size * 5)
    val scoreClamped = trustScore.coerceIn(0, 100)
    val isPerfect = scoreClamped == 100

    val primaryColor = Color(0xFF00D4FF)
    val themeColor = if (isPerfect) Color(0xFF37DF66) else Color(0xFFF44336)
    
    val bgColor = Color(0xFF05070A)
    val surfaceColor = Color(0xFF10141A)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "scale"
    )

    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // TopAppBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor.copy(alpha = 0.8f))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = primaryColor, modifier = Modifier.size(24.dp).clickable { navController?.popBackStack() })
                Spacer(Modifier.width(16.dp))
                Text("Trust Check", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = primaryColor)
            }
            GKInfoButton(color = primaryColor) { showInfoDialog = true }
        }

        // Tabs
        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabs = listOf("User Installed", "System Installed")

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = bgColor,
            contentColor = primaryColor
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.Bold, color = if (selectedTabIndex == index) primaryColor else LocalGKColors.current.textSecondary) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp)
        ) {
            // Score Header Section
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier.size(192.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(192.dp)
                                .scale(pulseScale)
                                .background(Brush.radialGradient(listOf(themeColor.copy(alpha = 0.25f), Color.Transparent)))
                        )
                        
                        val progressAnim = remember { Animatable(0f) }
                        LaunchedEffect(scoreClamped) {
                            progressAnim.animateTo(scoreClamped / 100f, animationSpec = tween(1500, easing = FastOutSlowInEasing))
                        }
                        
                        Canvas(modifier = Modifier.size(180.dp).rotate(-90f)) {
                            drawArc(
                                color = Color.White.copy(alpha = 0.05f), startAngle = 0f, sweepAngle = 360f, useCenter = false,
                                style = Stroke(width = 8.dp.toPx())
                            )
                            drawArc(
                                color = themeColor, startAngle = 0f, sweepAngle = 360f * progressAnim.value, useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "$scoreClamped", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = themeColor)
                            Text(text = "/ 100", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textSecondary)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (isPerfect) "Device is Secure" else "Review Suggested",
                        style = MaterialTheme.typography.titleLarge, color = LocalGKColors.current.textPrimary
                    )
                    Text(
                        if (isPerfect) "No untrusted certificates detected." else "${userCerts.size} user-installed certificates detected",
                        style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textSecondary, modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            val currentList = if (selectedTabIndex == 0) userCerts else systemCerts

            if (currentList.isEmpty()) {
                item {
                    Text(
                        "No certificates found in this category.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalGKColors.current.textSecondary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (selectedTabIndex == 0) "USER CERTIFICATES" else "SYSTEM CERTIFICATES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textSecondary, letterSpacing = 0.5.sp)
                            if (selectedTabIndex == 0) {
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Filled.Info, null, tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Text("${currentList.size} Active", fontSize = 14.sp, color = LocalGKColors.current.textTertiary)
                    }
                }

                items(currentList) { cert ->
                    CertRow(cert = cert, onClick = { selectedCert = cert })
                }
            }
        }
    }

    if (selectedCert != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedCert = null },
            containerColor = surfaceColor,
            contentColor = LocalGKColors.current.textPrimary
        ) {
            CertDetailView(selectedCert!!)
        }
    }

    if (showInfoDialog) {
        GKInfoDialog(
            title = "Trust Check",
            body = "Trust Check scans your device for installed SSL certificates.\n\nMalicious apps or compromised networks can install 'Rogue Certificates' which allow them to secretly intercept and read your secure HTTPS traffic (like banking or passwords).\n\n• System Certificates: Installed by Android, generally safe.\n• User Certificates: Installed by you or third-party apps, higher risk and should be reviewed.",
            accentColor = primaryColor,
            onDismiss = { showInfoDialog = false }
        )
    }
}

@Composable
fun CertRow(cert: CertificateInfo, onClick: () -> Unit) {
    val isHighRisk = cert.detectionConfidence == "HIGH" && cert.isUserCertificate
    val riskColor = if (cert.isExpired) Color.Gray else if (isHighRisk) Color(0xFFF44336) else if (cert.isUserCertificate) Color(0xFFFFC107) else Color(0xFF37DF66)
    val icon = if (isHighRisk) Icons.Filled.AdminPanelSettings else if (cert.isUserCertificate) Icons.Filled.Security else Icons.Filled.VerifiedUser

    val format = remember { java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(riskColor.copy(alpha = 0.1f)).border(1.dp, riskColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = riskColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(cert.alias, style = MaterialTheme.typography.bodyLarge, color = LocalGKColors.current.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Issuer: ${cert.issuerName}", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Expires: ${format.format(java.util.Date(cert.validUntil))}", style = MaterialTheme.typography.labelSmall, color = LocalGKColors.current.textTertiary)
        }
    }
}

@Composable
fun CertDetailView(cert: CertificateInfo) {
    val format = remember { java.text.SimpleDateFormat("MMM dd, yyyy HH:mm:ss", java.util.Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text("Certificate Details", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            item { DetailItem("Alias", cert.alias) }
            item { DetailItem("Type", if (cert.isSystemCertificate) "System" else "User-Installed") }
            item { DetailItem("Status", cert.trustStatus) }
            item { DetailItem("Subject DN", cert.subjectName) }
            item { DetailItem("Issuer DN", cert.issuerName) }
            item { DetailItem("Serial Number", cert.serialNumber ?: "N/A") }
            item { DetailItem("Signature Algorithm", cert.signatureAlgorithm) }
            item { DetailItem("Public Key Algorithm", cert.publicKeyAlgorithm) }
            item { DetailItem("Valid From", format.format(java.util.Date(cert.validFrom))) }
            item { DetailItem("Valid Until", format.format(java.util.Date(cert.validUntil))) }
            item { DetailItem("SHA-1 Fingerprint", cert.sha1Fingerprint) }
            item { DetailItem("SHA-256 Fingerprint", cert.sha256Fingerprint) }

            if (cert.detectedTrustingApps.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Apps with specific trust configuration:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textPrimary)
                    cert.detectedTrustingApps.forEach { appName ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowRight, null, tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(16.dp))
                            Text(appName, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Raw PEM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textPrimary)
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.3f)).padding(8.dp)) {
                    Text(cert.pemEncoded, fontSize = 10.sp, color = LocalGKColors.current.textSecondary, style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace))
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LocalGKColors.current.textSecondary, letterSpacing = 0.5.sp)
        Text(value, fontSize = 14.sp, color = LocalGKColors.current.textPrimary)
    }
}
