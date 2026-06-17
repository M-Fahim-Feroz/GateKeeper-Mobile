package com.gatekeeper.mobile.ui.screens.onboarding

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gatekeeper.mobile.ui.components.*
import com.gatekeeper.mobile.ui.theme.*

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalGKColors.current.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (targetStep) {
                    0 -> StepWelcome { viewModel.nextStep() }
                    1 -> StepVpn(viewModel)
                    2 -> StepProtection(viewModel)
                    3 -> StepDone(viewModel, onComplete)
                }
            }
        }
        
        Row(
            modifier = Modifier.padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            for (i in 0..3) {
                val color by animateColorAsState(if (i == currentStep) LocalGKColors.current.primary else LocalGKColors.current.border)
                val width by animateDpAsState(if (i == currentStep) 24.dp else 8.dp)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = width, height = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun StepWelcome(onNext: () -> Unit) {
    Image(
        painter = painterResource(id = if (androidx.compose.foundation.isSystemInDarkTheme()) com.gatekeeper.mobile.R.drawable.gk_logo_dark else com.gatekeeper.mobile.R.drawable.gk_logo_light),
        contentDescription = "GateKeeper Logo",
        modifier = Modifier.size(120.dp)
    )
    Spacer(modifier = Modifier.height(24.dp))
    Text("Mobile Security Suite", style = MaterialTheme.typography.headlineMedium, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    Text("Protect your device in 60 seconds", style = MaterialTheme.typography.bodyLarge, color = LocalGKColors.current.textSecondary)
    Spacer(modifier = Modifier.height(48.dp))
    GKPrimaryButton("Get Started", onClick = onNext)
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

    Icon(Icons.Filled.Shield, contentDescription = null, tint = LocalGKColors.current.accentOrange, modifier = Modifier.size(80.dp))
    Spacer(modifier = Modifier.height(24.dp))
    Text("Enable Protection", style = MaterialTheme.typography.headlineMedium, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    Text("GateKeeper uses a local VPN — your traffic never leaves your device.", style = MaterialTheme.typography.bodyLarge, color = LocalGKColors.current.textSecondary, textAlign = TextAlign.Center)
    
    Spacer(modifier = Modifier.height(48.dp))

    when (vpnState) {
        VpnPermState.UNKNOWN -> {
            GKPrimaryButton("Enable Protection", onClick = requestVpn)
        }
        VpnPermState.DENIED -> {
            Column(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(LocalGKColors.current.surfaceVariant).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("GateKeeper needs this to block apps. Without it, monitoring only — no blocking.", color = LocalGKColors.current.textPrimary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                GKPrimaryButton("Try Again", onClick = requestVpn)
                Spacer(modifier = Modifier.height(8.dp))
                GKOutlineButton("Continue without VPN", onClick = { viewModel.nextStep() })
            }
        }
        VpnPermState.PERMANENTLY_DENIED -> {
            Column(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(LocalGKColors.current.surfaceVariant).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Open Settings → Apps → GateKeeper → Permissions → VPN", color = LocalGKColors.current.textPrimary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                GKPrimaryButton("Open Settings", onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                })
                Spacer(modifier = Modifier.height(8.dp))
                GKOutlineButton("Continue without VPN", onClick = { viewModel.nextStep() })
            }
        }
        VpnPermState.GRANTED -> {
            // Should auto-advance, but just in case
            GKPrimaryButton("Continue", onClick = { viewModel.nextStep() })
        }
    }
}

@Composable
fun StepProtection(viewModel: OnboardingViewModel) {
    val selected by viewModel.selectedProtectionLevel.collectAsState()

    Text("Choose Protection Level", style = MaterialTheme.typography.headlineMedium, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))

    ProtectionCard(
        title = "Basic",
        description = "VPN on only",
        isSelected = selected == "basic",
        onClick = { viewModel.selectProtectionLevel("basic") }
    )
    Spacer(modifier = Modifier.height(12.dp))
    ProtectionCard(
        title = "Standard",
        description = "VPN + Default DNS blocklist",
        isSelected = selected == "standard",
        onClick = { viewModel.selectProtectionLevel("standard") }
    )
    Spacer(modifier = Modifier.height(12.dp))
    ProtectionCard(
        title = "Advanced",
        description = "VPN + Threat feeds + Detection engines",
        isSelected = selected == "advanced",
        onClick = { viewModel.selectProtectionLevel("advanced") },
        extraInfo = "Will enable: IMSI detection, Evil Twin scanning, all threat feed imports (requires internet)"
    )

    Spacer(modifier = Modifier.height(48.dp))
    GKPrimaryButton("Continue", onClick = { viewModel.nextStep() })
}

@Composable
fun ProtectionCard(title: String, description: String, isSelected: Boolean, onClick: () -> Unit, extraInfo: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) LocalGKColors.current.primary.copy(alpha = 0.1f) else LocalGKColors.current.card)
            .border(2.dp, if (isSelected) LocalGKColors.current.primary else LocalGKColors.current.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = if (isSelected) LocalGKColors.current.primary else LocalGKColors.current.textPrimary, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textSecondary)
            }
            if (isSelected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = LocalGKColors.current.primary)
            }
        }
        if (isSelected && extraInfo != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(extraInfo, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.accentOrange)
        }
    }
}

@Composable
fun StepDone(viewModel: OnboardingViewModel, onComplete: () -> Unit) {
    // We mock a score of 85 if VPN is enabled.
    val vpnState by viewModel.vpnPermState.collectAsState()
    val isVpnOn = vpnState == VpnPermState.GRANTED
    
    val shieldColor by animateColorAsState(
        targetValue = if (isVpnOn) LocalGKColors.current.accentGreen else LocalGKColors.current.accentOrange,
        animationSpec = tween(800)
    )

    Icon(Icons.Filled.Shield, contentDescription = null, tint = shieldColor, modifier = Modifier.size(100.dp))
    Spacer(modifier = Modifier.height(24.dp))
    Text("You're Protected", style = MaterialTheme.typography.headlineMedium, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    Text("Your device is now secured by GateKeeper.", style = MaterialTheme.typography.bodyLarge, color = LocalGKColors.current.textSecondary)
    
    Spacer(modifier = Modifier.height(48.dp))
    GKPrimaryButton("Go to Dashboard", onClick = { viewModel.completeOnboarding(onComplete) })
}
