package com.gatekeeper.mobile.ui.theme

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.glassAmbientBackground(isDark: Boolean = isSystemInDarkTheme()): Modifier {
    return if (isDark) {
        this.background(DarkBackground)
    } else {
        this.drawBehind {
            drawRect(color = LightBackground)
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1E0EA5E9), Color.Transparent),
                    center = Offset(size.width * 0.2f, 0f),
                    radius = size.width * 0.45f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x1E3B82F6), Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.2f),
                    radius = size.width * 0.4f
                )
            )
        }
    }
}

@Composable
fun Modifier.glassPanel(isDark: Boolean = isSystemInDarkTheme(), shape: Shape = RoundedCornerShape(12.dp)): Modifier {
    val bgColor = if (isDark) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.22f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFF94A3B8).copy(alpha = 0.35f)
    
    return this
        .clip(shape)
        .then(if (!isDark && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(20.dp, BlurredEdgeTreatment.Rectangle) else Modifier)
        .background(if (!isDark && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) Color.White.copy(alpha = 0.7f) else bgColor)
        .border(1.dp, borderColor, shape)
}

@Composable
fun Modifier.glassCard(isDark: Boolean = isSystemInDarkTheme(), shape: Shape = RoundedCornerShape(12.dp)): Modifier {
    val bgColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.7f)
    val borderColor = if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFF94A3B8).copy(alpha = 0.2f)
    return this
        .clip(shape)
        .background(bgColor)
        .border(1.dp, borderColor, shape)
}

@Composable
fun Modifier.glassInput(isDark: Boolean = isSystemInDarkTheme(), isFocused: Boolean = false, shape: Shape = RoundedCornerShape(12.dp)): Modifier {
    val bgColor = if (isDark) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.8f)
    
    val defaultBorderColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFF94A3B8).copy(alpha = 0.45f)
    val focusedBorderColor = if (isDark) DarkPrimary.copy(alpha = 0.5f) else LightPrimary.copy(alpha = 0.5f)
    
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
