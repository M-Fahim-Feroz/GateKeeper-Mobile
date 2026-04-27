package com.gatekeeper.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary Brand Colors ──
val PrimaryCyan    = Color(0xFF00E5FF)
val PrimaryBlue    = Color(0xFF2979FF)
val SecondaryPurple = Color(0xFFAB47BC)
val AccentGreen    = Color(0xFF00E676)
val AccentRed      = Color(0xFFFF5252)
val AccentOrange   = Color(0xFFFF8C42)
val AccentYellow   = Color(0xFFFFD740)
val AccentTeal     = Color(0xFF1DE9B6)

// ── Dark Theme Surfaces ──
val DarkBackground       = Color(0xFF080C18)
val DarkSurface          = Color(0xFF0E1525)
val DarkSurfaceVariant   = Color(0xFF151E30)
val DarkCard             = Color(0xFF1A2438)
val DarkCardElevated     = Color(0xFF21304A)
val DarkCardHover        = Color(0xFF263856)

// ── Glass Effect ──
val GlassBackground  = Color(0x18FFFFFF)   // 9% white
val GlassBorder      = Color(0x26FFFFFF)   // 15% white
val GlassBorderFocus = Color(0x5500E5FF)   // Cyan tint
val GlassHighlight   = Color(0x0AFFFFFF)   // 4% white

// ── Text Colors ──
val TextPrimary   = Color(0xFFF0F6FF)
val TextSecondary = Color(0xFF8BA3C1)
val TextTertiary  = Color(0xFF526480)
val TextOnPrimary = Color(0xFF080C18)

// ── Status Colors ──
val StatusOnline  = Color(0xFF00E676)
val StatusOffline = Color(0xFFFF5252)
val StatusWarning = Color(0xFFFF8C42)
val StatusBlocked = Color(0xFFFF5252)
val StatusAllowed = Color(0xFF00E676)

// ── Gradient Presets (2-stop) ──
val GradientPrimary = listOf(PrimaryCyan, PrimaryBlue)
val GradientDanger  = listOf(Color(0xFFFF5252), Color(0xFFD32F2F))
val GradientSuccess = listOf(AccentGreen, Color(0xFF00B248))
val GradientPurple  = listOf(Color(0xFFCE93D8), Color(0xFF7C4DFF))
val GradientOrange  = listOf(AccentOrange, AccentYellow)
val GradientTeal    = listOf(AccentTeal, PrimaryCyan)
val GradientCard    = listOf(DarkCard, DarkCardElevated)

val BorderDefault   = GlassBorder
val DarkBg          = DarkBackground
val SurfaceDefault  = DarkSurface

