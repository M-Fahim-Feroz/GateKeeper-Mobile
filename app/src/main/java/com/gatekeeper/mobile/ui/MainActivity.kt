package com.gatekeeper.mobile.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.gatekeeper.mobile.data.repository.SettingsRepository
import com.gatekeeper.mobile.ui.navigation.AppNavigation
import com.gatekeeper.mobile.ui.theme.GateKeeperTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import android.content.Intent
import android.net.VpnService
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.gatekeeper.mobile.vpn.GateKeeperVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val _deepLinkRoute = MutableStateFlow<String?>(null)

    // Launcher for VPN permission dialog
    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // User granted VPN permission — now start it
        startVpnService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        _deepLinkRoute.value = intent.getStringExtra("target_route")

        // Auto-start VPN once onboarding is complete and auto-start preference is on
        lifecycleScope.launch {
            val onboardingDone = settingsRepository.onboardingDoneFlow.first()
            val autoStart = settingsRepository.autoVpnStartFlow.first()
            val vpnAlreadyRunning = GateKeeperVpnService.isRunning.value

            if (onboardingDone && autoStart && !vpnAlreadyRunning) {
                val prepareIntent = VpnService.prepare(this@MainActivity)
                if (prepareIntent != null) {
                    vpnPermissionLauncher.launch(prepareIntent)
                } else {
                    startVpnService()
                }
            }
        }

        setContent {
            val onboardingDone by settingsRepository.onboardingDoneFlow.collectAsState(initial = false)
            val deepLink by _deepLinkRoute.collectAsState()
            val themeMode by settingsRepository.themeModeFlow.collectAsState(initial = 0)

            val isDark = when (themeMode) {
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            GateKeeperTheme(darkTheme = isDark) {
                AppNavigation(
                    startDestination = if (onboardingDone) "dashboard" else "onboarding",
                    deepLinkRoute = deepLink,
                    onDeepLinkConsumed = { _deepLinkRoute.value = null }
                )
            }
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, GateKeeperVpnService::class.java).apply {
            action = GateKeeperVpnService.ACTION_START
        }
        startForegroundService(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _deepLinkRoute.value = intent.getStringExtra("target_route")
    }
}
