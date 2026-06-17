package com.gatekeeper.mobile.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class GKColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val card: Color,
    val cardElevated: Color,
    val cardHover: Color,
    val border: Color,
    val borderFocus: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textOnPrimary: Color,
    val primary: Color,
    val accentRed: Color,
    val accentGreen: Color,
    val accentOrange: Color,
    val accentYellow: Color,
    val accentTeal: Color,
    val glassBg: Color,
    val statusOnline: Color,
    val statusOffline: Color,
    val statusWarning: Color,
    val statusBlocked: Color,
    val statusAllowed: Color,
    val isLight: Boolean
)

val darkGKColors = GKColors(
    background = Color(0xFF09090B),
    surface = Color(0xFF0E1525),
    surfaceVariant = Color(0xFF151E30),
    card = Color(0xFF1A2438),
    cardElevated = Color(0xFF21304A),
    cardHover = Color(0xFF263856),
    border = Color(0x26FFFFFF),
    borderFocus = Color(0x5500E5FF),
    textPrimary = Color(0xFFF0F6FF),
    textSecondary = Color(0xFF8BA3C1),
    textTertiary = Color(0xFF526480),
    textOnPrimary = Color(0xFF080C18),
    primary = Color(0xFF00E5FF),
    accentRed = Color(0xFFFF5252),
    accentGreen = Color(0xFF00E676),
    accentOrange = Color(0xFFFF8C42),
    accentYellow = Color(0xFFFFD740),
    accentTeal = Color(0xFF1DE9B6),
    glassBg = Color(0x18FFFFFF),
    statusOnline = Color(0xFF00E676),
    statusOffline = Color(0xFFFF5252),
    statusWarning = Color(0xFFFF8C42),
    statusBlocked = Color(0xFFFF5252),
    statusAllowed = Color(0xFF00E676),
    isLight = false
)

val lightGKColors = GKColors(
    background = Color(0xFFF0F2FF), // Indigo-tinted white for glass base
    surface = Color(0xCCFFFFFF), // 80% white for translucent surface
    surfaceVariant = Color(0xFFF8FAFC),
    card = Color(0xA6FFFFFF), // 65% white so cards appear to float
    cardElevated = Color(0xB3FFFFFF), // 70% white elevated
    cardHover = Color(0xBFFFFFFF), // 75% white hover
    border = Color(0x336366F1), // 20% indigo for borders
    borderFocus = Color(0xFF00BCD4), // Cyber teal
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textTertiary = Color(0xFF64748B),
    textOnPrimary = Color(0xFFFFFFFF),
    primary = Color(0xFF0891B2), // Cyber teal primary
    accentRed = Color(0xFFEF4444),
    accentGreen = Color(0xFF059669), // Deeper emerald
    accentOrange = Color(0xFFF59E0B),
    accentYellow = Color(0xFFFBBF24),
    accentTeal = Color(0xFF14B8A6),
    glassBg = Color(0x8CFFFFFF), // 55% white for better readability
    statusOnline = Color(0xFF10B981),
    statusOffline = Color(0xFFEF4444),
    statusWarning = Color(0xFFF59E0B),
    statusBlocked = Color(0xFFEF4444),
    statusAllowed = Color(0xFF10B981),
    isLight = true
)

val LocalGKColors = staticCompositionLocalOf { darkGKColors }

// ── Legacy Colors (Restored for Compilation during migration) ──
val PrimaryCyan    = Color(0xFF00E5FF)
val PrimaryBlue    = Color(0xFF2979FF)
val SecondaryPurple = Color(0xFFAB47BC)
val AccentGreen    = Color(0xFF00E676)
val AccentRed      = Color(0xFFFF5252)
val AccentOrange   = Color(0xFFFF8C42)
val AccentYellow   = Color(0xFFFFD740)
val AccentTeal     = Color(0xFF1DE9B6)

val DarkSurface          = Color(0xFF0E1525)
val DarkSurfaceVariant   = Color(0xFF151E30)
val DarkCard             = Color(0xFF1A2438)
val DarkCardElevated     = Color(0xFF21304A)
val DarkCardHover        = Color(0xFF263856)

val GlassBackground  = Color(0x18FFFFFF)
val GlassBorder      = Color(0x26FFFFFF)
val GlassBorderFocus = Color(0x5500E5FF)
val GlassHighlight   = Color(0x0AFFFFFF)

val LightOrb1 = Color(0x1A6366F1)  // Indigo orb — top-left
val LightOrb2 = Color(0x1A0891B2)  // Teal orb — top-right  
val LightOrb3 = Color(0x14A855F7)  // Purple orb — bottom

val TextPrimary   = Color(0xFFF0F6FF)
val TextSecondary = Color(0xFF8BA3C1)
val TextTertiary  = Color(0xFF526480)
val TextOnPrimary = Color(0xFF080C18)

val GradientPrimary = listOf(PrimaryCyan, PrimaryBlue)
val GradientDanger  = listOf(Color(0xFFFF5252), Color(0xFFD32F2F))
val GradientSuccess = listOf(AccentGreen, Color(0xFF00B248))
val GradientPurple  = listOf(Color(0xFFCE93D8), Color(0xFF7C4DFF))
val GradientOrange  = listOf(AccentOrange, AccentYellow)
val GradientTeal    = listOf(AccentTeal, PrimaryCyan)
val GradientCard    = listOf(DarkCard, DarkCardElevated)

val BorderDefault   = GlassBorder
val DarkBg          = Color(0xFF080C18)
val SurfaceDefault  = DarkSurface

val DarkBackground = Color(0xFF09090B)
val DarkForeground = Color(0xFFFAFAFA)
val DarkPrimary = Color(0xFF00FFFF)
val DarkPrimaryForeground = Color(0xFF18181B)
val DarkSecondary = Color(0xFF7F00FF)
val DarkMuted = Color(0xFF27272A)
val DarkMutedForeground = Color(0xFFA1A1AA)
val DarkDestructive = Color(0xFF7F1D1D)
val DarkBorder = Color(0xFF27272A)

val LightBackground = Color(0xFFF8FAFC)
val LightForeground = Color(0xFF0F172A)
val LightPrimary = Color(0xFF007BFF)
val LightPrimaryForeground = Color(0xFFFAFAFA)
val LightSecondary = Color(0xFF8FA7C6)
val LightMuted = Color(0xFFF1F5F9)
val LightMutedForeground = Color(0xFF64748B)
val LightDestructive = Color(0xFFFF3333)
val LightBorder = Color(0xFFDDE3EA)

val StatusOnline  = Color(0xFF00E676)
val StatusOffline = Color(0xFFFF5252)
val StatusWarning = Color(0xFFFF8C42)
val StatusBlocked = Color(0xFFFF5252)
val StatusAllowed = Color(0xFF00E676)
