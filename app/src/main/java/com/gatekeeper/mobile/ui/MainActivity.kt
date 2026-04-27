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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val onboardingDone by settingsRepository.onboardingDoneFlow.collectAsState(initial = false)
            GateKeeperTheme {
                AppNavigation(startDestination = if (onboardingDone) "dashboard" else "onboarding")
            }
        }
    }
}
