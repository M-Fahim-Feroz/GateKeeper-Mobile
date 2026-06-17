package com.gatekeeper.mobile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GateKeeperDarkScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkPrimaryForeground,
    primaryContainer = DarkMuted,
    onPrimaryContainer = DarkPrimary,
    secondary = DarkSecondary,
    onSecondary = DarkForeground,
    background = DarkBackground,
    onBackground = DarkForeground,
    surface = DarkBackground,
    onSurface = DarkForeground,
    surfaceVariant = DarkMuted,
    onSurfaceVariant = DarkMutedForeground,
    error = DarkDestructive,
    onError = DarkForeground,
    outline = DarkBorder,
    outlineVariant = DarkBorder
)

private val GateKeeperLightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightPrimaryForeground,
    primaryContainer = LightMuted,
    onPrimaryContainer = LightPrimary,
    secondary = LightSecondary,
    onSecondary = LightForeground,
    background = LightBackground,
    onBackground = LightForeground,
    surface = LightBackground,
    onSurface = LightForeground,
    surfaceVariant = LightMuted,
    onSurfaceVariant = LightMutedForeground,
    error = LightDestructive,
    onError = LightForeground,
    outline = LightBorder,
    outlineVariant = LightBorder
)

@Composable
fun GateKeeperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        GateKeeperDarkScheme
    } else {
        GateKeeperLightScheme
    }
    
    val gkColors = if (darkTheme) darkGKColors else lightGKColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = androidx.compose.ui.graphics.Color.Transparent.toArgb()
            window.navigationBarColor = androidx.compose.ui.graphics.Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalGKColors provides gkColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GateKeeperTypography,
            content = content
        )
    }
}
