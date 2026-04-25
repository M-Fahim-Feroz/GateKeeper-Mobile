package com.gatekeeper.mobile.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gatekeeper.mobile.ui.navigation.AppNavigation
import com.gatekeeper.mobile.ui.theme.GateKeeperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GateKeeperTheme {
                AppNavigation()
            }
        }
    }
}
