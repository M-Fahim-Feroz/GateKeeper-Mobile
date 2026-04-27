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
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val _deepLinkRoute = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        _deepLinkRoute.value = intent.getStringExtra("target_route")

        setContent {
            val onboardingDone by settingsRepository.onboardingDoneFlow.collectAsState(initial = false)
            val deepLink by _deepLinkRoute.collectAsState()

            GateKeeperTheme {
                AppNavigation(
                    startDestination = if (onboardingDone) "dashboard" else "onboarding",
                    deepLinkRoute = deepLink,
                    onDeepLinkConsumed = { _deepLinkRoute.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        _deepLinkRoute.value = intent.getStringExtra("target_route")
    }
}
