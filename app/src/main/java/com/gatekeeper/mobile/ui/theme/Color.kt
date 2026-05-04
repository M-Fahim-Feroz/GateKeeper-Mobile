package com.gatekeeper.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// ── Legacy Colors (Restored for Compilation) ──
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
val DarkBg          = Color(0xFF080C18) // Original DarkBackground
val SurfaceDefault  = DarkSurface

// ── Dark Theme Colors (Cyberpunk) ──
val DarkBackground = Color(0xFF09090B)
val DarkForeground = Color(0xFFFAFAFA)
val DarkPrimary = Color(0xFF00FFFF)
val DarkPrimaryForeground = Color(0xFF18181B)
val DarkSecondary = Color(0xFF7F00FF)
val DarkMuted = Color(0xFF27272A)
val DarkMutedForeground = Color(0xFFA1A1AA)
val DarkDestructive = Color(0xFF7F1D1D)
val DarkBorder = Color(0xFF27272A)

// ── Light Theme Colors (Airy Blue) ──
val LightBackground = Color(0xFFF8FAFC)
val LightForeground = Color(0xFF0F172A)
val LightPrimary = Color(0xFF007BFF)
val LightPrimaryForeground = Color(0xFFFAFAFA)
val LightSecondary = Color(0xFF8FA7C6)
val LightMuted = Color(0xFFF1F5F9)
val LightMutedForeground = Color(0xFF64748B)
val LightDestructive = Color(0xFFFF3333)
val LightBorder = Color(0xFFDDE3EA)

// ── Status Colors ──
val StatusOnline  = Color(0xFF00E676)
val StatusOffline = Color(0xFFFF5252)
val StatusWarning = Color(0xFFFF8C42)
val StatusBlocked = Color(0xFFFF5252)
val StatusAllowed = Color(0xFF00E676)
