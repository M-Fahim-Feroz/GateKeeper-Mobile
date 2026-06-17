package com.gatekeeper.mobile.ui.screens.wifiscanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

    Column(modifier = Modifier.fillMaxSize().background(LocalGKColors.current.background)) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(LocalGKColors.current.primary.copy(alpha = 0.08f), LocalGKColors.current.background)))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            if (navController != null) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = LocalGKColors.current.textSecondary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(GradientTeal.map { it.copy(alpha = 0.2f) })),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.WifiTethering, null, tint = LocalGKColors.current.accentTeal, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Wi-Fi Scanner", style = MaterialTheme.typography.displaySmall, color = LocalGKColors.current.textPrimary)
                    Text("Analyze local network security", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
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
                    containerColor = LocalGKColors.current.primary,
                    contentColor = LocalGKColors.current.background,
                    disabledContainerColor = LocalGKColors.current.surfaceVariant
                )
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalGKColors.current.textTertiary)
                    Spacer(Modifier.width(12.dp))
                    Text("Scanning Network...", color = LocalGKColors.current.textTertiary, fontWeight = FontWeight.Bold)
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
                    Icon(Icons.Filled.Wifi, null, tint = LocalGKColors.current.textTertiary, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("No scan results yet", color = LocalGKColors.current.textSecondary, style = MaterialTheme.typography.bodyMedium)
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
            containerColor = LocalGKColors.current.card,
            title = { Text("Location Required", color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Android explicitly requires Location Permissions for apps to scan Wi-Fi networks in order to evaluate their encryption security.", color = LocalGKColors.current.textSecondary) },
            confirmButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Understood", color = LocalGKColors.current.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun DeviceItem(result: WifiNetworkInfo) {
    var expanded by remember { mutableStateOf(false) }

    val leftBorderColor = when {
        result.isEvilTwin  -> LocalGKColors.current.accentRed
        result.isSuspicious -> LocalGKColors.current.accentOrange
        else               -> LocalGKColors.current.accentGreen.copy(alpha = 0f) // invisible for clean networks
    }

    val badgeText = when {
        result.isEvilTwin   -> "Evil Twin"
        result.isSuspicious -> "Suspicious"
        result.securityType == "OPEN" -> "No Password"
        else                -> "Secure"
    }
    val badgeColor = when {
        result.isEvilTwin   -> LocalGKColors.current.accentRed
        result.isSuspicious -> LocalGKColors.current.accentOrange
        result.securityType == "OPEN" -> LocalGKColors.current.accentRed
        else                -> LocalGKColors.current.accentGreen
    }

    val wifiIcon = Icons.Filled.Wifi
    val signalTint = when {
        result.isEvilTwin || result.isSuspicious -> badgeColor
        result.securityType == "OPEN" -> LocalGKColors.current.accentOrange
        else -> LocalGKColors.current.accentGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LocalGKColors.current.card)
            .then(
                if (result.isEvilTwin || result.isSuspicious)
                    Modifier.border(1.dp, leftBorderColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                else Modifier
            )
            .clickable(enabled = result.isEvilTwin || result.isSuspicious) { expanded = !expanded }
    ) {
        // Left accent bar for threats
        if (result.isEvilTwin || result.isSuspicious) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(leftBorderColor, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(signalTint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(wifiIcon, null, tint = signalTint, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        result.ssid.ifEmpty { "Hidden Network" },
                        style = MaterialTheme.typography.titleSmall,
                        color = LocalGKColors.current.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        buildString {
                            append(result.bssid)
                            if (result.vendorName != null) append(" · ${result.vendorName}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalGKColors.current.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        badgeText.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Expandable threat detail for evil twin / suspicious
            if ((result.isEvilTwin || result.isSuspicious) && expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = LocalGKColors.current.border)
                Spacer(Modifier.height(12.dp))
                val detailText = if (result.isEvilTwin)
                    "This network matches a known Wi-Fi name but uses a different MAC address. This may be an Evil Twin access point intercepting your traffic.\n\nRecommendation: Disconnect immediately and use mobile data."
                else
                    "This is an unknown network broadcasting an unusually strong signal. It may be a hotspot set up to capture your traffic.\n\nRecommendation: Verify the network with the venue before connecting."
                Text(
                    detailText,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalGKColors.current.textSecondary
                )
            }
        }
    }
}

@Composable
fun KnownNetworkItem(network: KnownNetwork, onTrust: () -> Unit) {
    val isTrusted = network.isTrusted
    val scoreColor = if (isTrusted) LocalGKColors.current.accentGreen else LocalGKColors.current.accentRed
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(LocalGKColors.current.card)
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
                Text(network.ssid, style = MaterialTheme.typography.titleMedium, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.SemiBold)
                Text(network.bssid, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
            }
            
            if (!isTrusted) {
                TextButton(onClick = onTrust) {
                    Text("Trust", color = LocalGKColors.current.primary, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(scoreColor.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text("Trusted", style = MaterialTheme.typography.labelSmall, color = scoreColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
