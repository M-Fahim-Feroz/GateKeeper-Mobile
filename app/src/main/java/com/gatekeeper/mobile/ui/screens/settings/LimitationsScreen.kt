package com.gatekeeper.mobile.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.gatekeeper.mobile.ui.theme.LocalGKColors
import com.gatekeeper.mobile.ui.theme.glassAmbientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitationsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Technical Transparency", color = LocalGKColors.current.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LocalGKColors.current.textPrimary)
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
                .glassAmbientBackground()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(LocalGKColors.current.primary.copy(alpha = 0.1f), LocalGKColors.current.background)))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        "OS Restrictions & Architecture",
                        style = MaterialTheme.typography.titleLarge,
                        color = LocalGKColors.current.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    com.gatekeeper.mobile.ui.components.GKBadge("TECHNICAL TRANSPARENCY", com.gatekeeper.mobile.ui.components.BadgeStyle.MEDIUM)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "GateKeeper pushes the boundaries of Android security. However, due to Android's sandboxing and OS-level restrictions, certain features use simulated data for demonstration purposes or employ alternative architectural approaches. These are documented platform limitations, not implementation gaps.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalGKColors.current.textSecondary
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                AccordionItem(
                    title = "Live Mic/Camera Tracking",
                    icon = Icons.Filled.Mic,
                    badge = "OS LIMITED",
                    content = "Android 10+ heavily restricted the AppOpsManager. Specifically, the WATCH_APPOPS permission (needed to track which background app uses the camera or mic in real-time) requires 'Signature' level privileges. This means only apps signed with the OS manufacturer's key can read this data natively. In a non-rooted production environment, we use simulated data logs to demonstrate how the UI and alerting systems respond to background sensor abuse."
                )

                Spacer(Modifier.height(12.dp))

                AccordionItem(
                    title = "Real-Time TLS Decryption",
                    icon = Icons.Filled.Security,
                    badge = "ARCH DECISION",
                    content = "GateKeeper operates as a local VPN interface at OSI Layer 3/4 (Network/Transport). We can see IPs, ports, and domains (via SNI routing). However, Layer 7 payload decryption (like reading HTTPS packets) requires a Man-in-the-Middle (MITM) proxy and installing a custom root CA on the device, which degrades device security. Instead of breaking TLS, we built the Certificate Auditor to scan the system trust store for malicious CAs installed by attackers."
                )

                Spacer(Modifier.height(12.dp))

                AccordionItem(
                    title = "IMSI Catcher Proof",
                    icon = Icons.Filled.CellTower,
                    badge = "OS LIMITED",
                    content = "Android completely abstracts the hardware baseband (modem) chip from standard user-space applications. Direct access to low-level cellular signaling (like identifying a fake Stingray tower) is blocked by the radio layer. Our approach relies on higher-level heuristics available via Android's Telephony API, specifically monitoring sudden network downgrades (e.g., forced drops from 4G/LTE to 2G) without corresponding geographic movement, which is a classic indicator of an IMSI catcher."
                )

                Spacer(Modifier.height(12.dp))

                AccordionItem(
                    title = "Hardware Sensor Killswitches",
                    icon = Icons.Filled.MicOff,
                    badge = "ARCH DECISION",
                    content = "In older versions of Android, Device Admin API could programmatically disable the camera or microphone. Google deprecated and removed this capability in recent Android versions to prevent malware from hijacking hardware toggles. Instead of a hardware block, GateKeeper's 'Global Block' feature relies on network-level exfiltration blocking. If an app attempts to use the mic and send data out, the VPN layer drops the traffic."
                )

                Spacer(Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, null, tint = LocalGKColors.current.textTertiary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("These notes are provided for FYP evaluation transparency.", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textTertiary)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AccordionItem(title: String, icon: ImageVector, badge: String, content: String) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "rotation"
    )

    com.gatekeeper.mobile.ui.components.GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                        .background(LocalGKColors.current.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = LocalGKColors.current.primary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, style = MaterialTheme.typography.titleMedium, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(if (badge == "OS LIMITED") LocalGKColors.current.accentOrange.copy(alpha=0.15f) else LocalGKColors.current.primary.copy(alpha=0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(badge, style = MaterialTheme.typography.labelSmall, color = if (badge == "OS LIMITED") LocalGKColors.current.accentOrange else LocalGKColors.current.primary, fontWeight = FontWeight.Bold)
                    }
                }
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = LocalGKColors.current.textTertiary,
                    modifier = Modifier.rotate(rotation)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = LocalGKColors.current.border)
                    Spacer(Modifier.height(12.dp))
                    Text(content, style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textSecondary, lineHeight = 20.sp)
                }
            }
        }
    }
}
