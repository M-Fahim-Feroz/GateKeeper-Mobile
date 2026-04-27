package com.gatekeeper.mobile.ui.screens.certaudit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.components.SectionHeader
import com.gatekeeper.mobile.ui.theme.*
import com.gatekeeper.mobile.vpn.RogueCertInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CertAuditScreen(
    navController: NavController,
    viewModel: CertAuditViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val certs by viewModel.certs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Certificate Auditor", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
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
                    .background(Brush.verticalGradient(listOf(AccentOrange.copy(alpha = 0.1f), DarkBackground)))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "SSL Trust Store",
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "GateKeeper scans your device for user-installed Root Certificate Authorities. These can be used to perform Man-in-the-Middle (MITM) attacks and decrypt your HTTPS traffic.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryCyan)
                }
            } else if (certs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Security, null, tint = AccentGreen, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Trust Store Clean", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("No user-installed certificates found. Your HTTPS connections are secure.", color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SectionHeader("User-Installed Certificates")
                        Spacer(Modifier.height(8.dp))
                    }
                    items(certs) { cert ->
                        CertItemCard(cert)
                    }
                }
            }
        }
    }
}

@Composable
fun CertItemCard(cert: RogueCertInfo) {
    val isHighRisk = cert.riskLevel == "HIGH"
    val color = if (isHighRisk) AccentRed else AccentOrange
    val icon = if (isHighRisk) Icons.Default.Warning else Icons.Default.Info

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
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
                        if (isHighRisk) "High Risk Certificate" else "User Certificate",
                        style = MaterialTheme.typography.titleMedium,
                        color = color,
                        fontWeight = FontWeight.Bold
                    )
                    Text(cert.alias, style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            Divider(color = GlassBorder)
            Spacer(Modifier.height(16.dp))
            
            Text("Issuer", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
            Text(cert.issuerName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            
            Spacer(Modifier.height(8.dp))
            Text("Subject", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
            Text(cert.subjectName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            
            Spacer(Modifier.height(8.dp))
            Text("Expires", style = MaterialTheme.typography.labelMedium, color = TextTertiary)
            Text(cert.expiresAt, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
    }
}
