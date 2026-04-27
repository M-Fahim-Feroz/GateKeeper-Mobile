package com.gatekeeper.mobile.ui.screens.wifiscanner

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
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.gatekeeper.mobile.domain.model.WifiNetworkInfo
import com.gatekeeper.mobile.data.db.entity.KnownNetwork
import com.gatekeeper.mobile.ui.components.*
import com.gatekeeper.mobile.ui.theme.*

@Composable
fun WifiScannerScreen(
    navController: NavController? = null,
    viewModel: WifiScannerViewModel = hiltViewModel()
) {
    val results by viewModel.scannedNetworks.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val knownNetworks by viewModel.knownNetworks.collectAsState(initial = emptyList())
    val context = LocalContext.current
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanWifi()
        } else {
            showPermissionDeniedDialog = true
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(PrimaryCyan.copy(alpha = 0.08f), DarkBackground)))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            if (navController != null) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(GradientTeal.map { it.copy(alpha = 0.2f) })),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.WifiTethering, null, tint = AccentTeal, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Wi-Fi Scanner", style = MaterialTheme.typography.displaySmall, color = TextPrimary)
                    Text("Analyze local network security", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = { 
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    
                    if (hasPermission) {
                        viewModel.scanWifi()
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryCyan,
                    contentColor = DarkBackground,
                    disabledContainerColor = DarkSurfaceVariant
                )
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = TextTertiary)
                    Spacer(Modifier.width(12.dp))
                    Text("Scanning Network...", color = TextTertiary, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.Search, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Scan Nearby Networks", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (results.isEmpty() && !isScanning) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Wifi, null, tint = TextTertiary, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No scan results yet", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { SectionHeader(title = "Discovered Networks") }
                items(results) { result -> DeviceItem(result) }
                
                if (knownNetworks.isNotEmpty()) {
                    item { Spacer(Modifier.height(16.dp)) }
                    item { SectionHeader(title = "Known Access Points") }
                    items(knownNetworks) { net -> KnownNetworkItem(net) { viewModel.trustNetwork(net) } }
                }
                
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            containerColor = DarkCard,
            title = { Text("Location Required", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Android explicitly requires Location Permissions for apps to scan Wi-Fi networks in order to evaluate their encryption security.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Understood", color = PrimaryCyan, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun DeviceItem(result: WifiNetworkInfo) {
    val isSecure = result.securityScore >= 7
    val scoreColor = if (isSecure) AccentGreen else AccentOrange
    val hostTypeIcon = Icons.Filled.Router
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(PrimaryCyan.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(hostTypeIcon, null, tint = PrimaryCyan, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(result.ssid.ifEmpty { "Hidden Network" }, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(result.bssid, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            
            Box(
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(scoreColor.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(result.securityType, style = MaterialTheme.typography.labelSmall, color = scoreColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun KnownNetworkItem(network: KnownNetwork, onTrust: () -> Unit) {
    val isTrusted = network.isTrusted
    val scoreColor = if (isTrusted) AccentGreen else AccentRed
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCard)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(scoreColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (isTrusted) Icons.Filled.VerifiedUser else Icons.Filled.Warning, null, tint = scoreColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(network.ssid, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(network.bssid, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            
            if (!isTrusted) {
                TextButton(onClick = onTrust) {
                    Text("Trust", color = PrimaryCyan, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(scoreColor.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("Trusted", style = MaterialTheme.typography.labelSmall, color = scoreColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
