package com.gatekeeper.mobile.ui.screens.onboarding

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gatekeeper.mobile.ui.components.*
import com.gatekeeper.mobile.ui.theme.*

@Composable
fun RadialBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
            .drawBehind {
                val radius = size.height * 0.85f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00D4FF).copy(alpha = 0.25f), Color(0xFF05070A)),
                        center = Offset(size.width / 2f, -size.height * 0.2f),
                        radius = radius
                    ),
                    radius = radius,
                    center = Offset(size.width / 2f, -size.height * 0.2f)
                )
            }
    ) {
        content()
    }
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    
    RadialBackground {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Nav
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { /* Back */ },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                
                Text(
                    "SKIP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalGKColors.current.textSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.clickable { onComplete() }
                )
            }

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(500)
                    ) togetherWith slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(500)
                    )
                },
                modifier = Modifier.weight(1f)
            ) { targetStep ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    when (targetStep) {
                        0 -> StepVpn(viewModel)
                        1 -> StepDone(viewModel, onComplete)
                        else -> StepVpn(viewModel)
                    }
                }
            }
            
            // Footer Progress
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 0..1) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (i <= currentStep) Color(0xFF00D4FF) else Color.White.copy(alpha = 0.2f))
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "STEP ${currentStep + 1} OF 2",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalGKColors.current.textSecondary.copy(alpha = 0.8f),
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun StepVpn(viewModel: OnboardingViewModel) {
    val context = LocalContext.current
    val vpnState by viewModel.vpnPermState.collectAsState()
    var cancelCount by remember { mutableStateOf(0) }

    val vpnLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.setVpnPermState(VpnPermState.GRANTED)
            viewModel.nextStep()
        } else {
            cancelCount++
            if (cancelCount >= 2) {
                viewModel.setVpnPermState(VpnPermState.PERMANENTLY_DENIED)
            } else {
                viewModel.setVpnPermState(VpnPermState.DENIED)
            }
        }
    }

    val requestVpn = {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            vpnLauncher.launch(intent)
        } else {
            viewModel.setVpnPermState(VpnPermState.GRANTED)
            viewModel.nextStep()
        }
    }

    val primaryColor = Color(0xFF00D4FF)
    
    // Rotating animation for hero
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(10000, easing = LinearEasing)), label = "rotate"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Element
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(bottom = 40.dp)) {
            // Rotating ring
            Box(
                modifier = Modifier
                    .size(192.dp)
                    .rotate(rotation)
                    .border(1.dp, primaryColor.copy(alpha = 0.2f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-4).dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                )
            }
            
            // Center icon
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF262A31).copy(alpha = 0.6f))
                    .border(1.dp, primaryColor.copy(alpha = 0.5f), CircleShape)
                    .drawBehind {
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.25f),
                            radius = size.width * 0.6f
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = primaryColor, modifier = Modifier.size(64.dp))
            }
        }

        // Typography
        Text("Grant VPN Permission", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Text(
            "To secure your connection and block threats, GateKeeper needs permission to establish a local VPN.",
            style = MaterialTheme.typography.bodyMedium,
            color = LocalGKColors.current.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(280.dp)
        )
        
        Spacer(Modifier.height(32.dp))

        // Detail Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.1f))
                .border(1.dp, primaryColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                // Feature 1
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF37DF66).copy(alpha = 0.25f))
                            .border(1.dp, Color(0xFF37DF66).copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Lock, null, tint = Color(0xFF37DF66), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Encrypts Traffic", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.Info, null, tint = LocalGKColors.current.textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Protects your data from snooping on public Wi-Fi networks.", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary.copy(alpha = 0.9f))
                    }
                }
                
                // Feature 2
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.25f))
                            .border(1.dp, primaryColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Shield, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Blocks Trackers", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.Info, null, tint = LocalGKColors.current.textSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Stops malicious domains before they can load on your device.", style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary.copy(alpha = 0.9f))
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))

        // Actions
        when (vpnState) {
            VpnPermState.UNKNOWN, VpnPermState.DENIED -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(primaryColor, Color(0xFF008EB3))))
                        .clickable(onClick = requestVpn),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VpnKey, null, tint = Color(0xFF05070A))
                        Spacer(Modifier.width(8.dp))
                        Text("Continue Setup", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF05070A))
                    }
                }
            }
            VpnPermState.PERMANENTLY_DENIED -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(primaryColor, Color(0xFF008EB3))))
                        .clickable {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Open Settings", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF05070A))
                }
            }
            VpnPermState.GRANTED -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(primaryColor, Color(0xFF008EB3))))
                        .clickable { viewModel.nextStep() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Continue", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF05070A))
                }
            }
        }
    }
}

// Keep the existing Done step so flow continues, but update styles subtly if needed.

@Composable
fun StepDone(viewModel: OnboardingViewModel, onComplete: () -> Unit) {
    val vpnState by viewModel.vpnPermState.collectAsState()
    val isVpnOn = vpnState == VpnPermState.GRANTED
    
    val shieldColor = if (isVpnOn) Color(0xFF37DF66) else Color(0xFFFF9800)
    val primaryColor = Color(0xFF00D4FF)

    Column(modifier = Modifier.padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Filled.Shield, contentDescription = null, tint = shieldColor, modifier = Modifier.size(120.dp))
        Spacer(modifier = Modifier.height(32.dp))
        Text("You're Protected", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Your device is now secured by GateKeeper.", style = MaterialTheme.typography.bodyLarge, color = LocalGKColors.current.textSecondary, textAlign = TextAlign.Center)
        
        Spacer(modifier = Modifier.height(48.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(primaryColor, Color(0xFF008EB3))))
                .clickable { viewModel.completeOnboarding(onComplete) },
            contentAlignment = Alignment.Center
        ) {
            Text("Go to Dashboard", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF05070A))
        }
    }
}
