package com.gatekeeper.mobile.ui.theme

import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ── Helper: read theme from CompositionLocal (respects user override in Settings) ──
// Using !isLight instead of isSystemInDarkTheme() means the Settings theme toggle works.
@Composable
private fun isDarkTheme(): Boolean = !LocalGKColors.current.isLight

@Composable
fun Modifier.glassAmbientBackground(): Modifier {
    val dark = isDarkTheme()
    return if (dark) {
        this.background(DarkBackground)
    } else {
        this.drawBehind {
            drawRect(color = LightBackground)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(LightOrb1, Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.1f),
                    radius = size.width * 0.6f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(LightOrb2, Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.2f),
                    radius = size.width * 0.5f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(LightOrb3, Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.8f),
                    radius = size.width * 0.7f
                )
            )
        }
    }
}

@Composable
fun Modifier.glassPanel(shape: Shape = RoundedCornerShape(12.dp)): Modifier {
    val dark = isDarkTheme()
    val bgColor = if (dark) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.72f)
    val borderColor = if (dark) Color.White.copy(alpha = 0.1f) else Color(0xFF6366F1).copy(alpha = 0.25f)

    return this
        .clip(shape)
        .background(bgColor)
        .border(1.dp, borderColor, shape)
        .drawBehind {
            if (!dark) {
                // Top edge highlight for glass effect
                drawRect(
                    color = Color.White.copy(alpha = 0.5f),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx())
                )
            }
        }
}

@Composable
fun Modifier.glassCard(shape: Shape = RoundedCornerShape(12.dp)): Modifier {
    val dark = isDarkTheme()
    val bgColor = if (dark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.72f)
    val borderColor = if (dark) Color.White.copy(alpha = 0.05f) else Color(0xFF6366F1).copy(alpha = 0.15f)
    return this
        .clip(shape)
        .background(bgColor)
        .border(1.dp, borderColor, shape)
        .drawBehind {
            if (!dark) {
                // Top edge highlight for glass effect
                drawRect(
                    color = Color.White.copy(alpha = 0.5f),
                    topLeft = Offset(0f, 0f),
                    size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx())
                )
            }
        }
}

@Composable
fun Modifier.glassInput(isFocused: Boolean = false, shape: Shape = RoundedCornerShape(12.dp)): Modifier {
    val dark = isDarkTheme()
    val bgColor = if (dark) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.8f)

    val defaultBorderColor = if (dark) Color.White.copy(alpha = 0.1f) else Color(0xFF94A3B8).copy(alpha = 0.45f)
    val focusedBorderColor = if (dark) DarkPrimary.copy(alpha = 0.5f) else LightPrimary.copy(alpha = 0.5f)

    val borderColor = if (isFocused) focusedBorderColor else defaultBorderColor

    return this
        .clip(shape)
        .background(bgColor)
        .border(1.dp, borderColor, shape)
}

@Composable
fun Modifier.glassButtonScale(interactionSource: MutableInteractionSource): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.95f else 1f, label = "Scale Animation")
    return this.scale(scale)
}

@Composable
fun Modifier.cyberGlowBorder(color: Color, cornerRadius: Dp, isActive: Boolean): Modifier {
    if (!isActive) return this

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    return this.border(
        width = 2.dp,
        color = color.copy(alpha = glowAlpha),
        shape = RoundedCornerShape(cornerRadius)
    )
}

@Composable
fun AmbientBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .glassAmbientBackground()
    ) {
        content()
    }
}

