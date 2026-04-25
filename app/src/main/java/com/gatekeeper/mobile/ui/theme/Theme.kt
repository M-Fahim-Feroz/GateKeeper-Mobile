package com.gatekeeper.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GateKeeperDarkScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = TextOnPrimary,
    primaryContainer = DarkCardElevated,
    onPrimaryContainer = PrimaryCyan,

    secondary = SecondaryPurple,
    onSecondary = TextOnPrimary,
    secondaryContainer = DarkCardElevated,
    onSecondaryContainer = SecondaryPurple,

    tertiary = AccentGreen,
    onTertiary = TextOnPrimary,

    background = DarkBackground,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    error = AccentRed,
    onError = TextOnPrimary,

    outline = GlassBorder,
    outlineVariant = GlassHighlight
)

@Composable
fun GateKeeperTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            window.navigationBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = GateKeeperDarkScheme,
        typography = GateKeeperTypography,
        content = content
    )
}
