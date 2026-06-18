package com.gatekeeper.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.gatekeeper.mobile.ui.theme.*

// 1. GKListRow
@Composable
fun GKListRow(
    icon: ImageVector,
    iconTint: Color = LocalGKColors.current.primary,
    title: String,
    subtitle: String,
    badge: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    var modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 70.dp)
        .glassCard(shape = RoundedCornerShape(12.dp))
    
    if (onClick != null) {
        modifier = modifier.clickable(onClick = onClick)
    }

    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = LocalGKColors.current.textPrimary, fontWeight = FontWeight.SemiBold)
                if (badge != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    GKBadge(badge, BadgeStyle.MEDIUM)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textSecondary)
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(16.dp))
            trailing()
        }
    }
}

// 2. GKToggle
@Composable
fun GKToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.size(width = 44.dp, height = 26.dp),
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            uncheckedThumbColor = Color.White,
            checkedTrackColor = LocalGKColors.current.primary,
            uncheckedTrackColor = LocalGKColors.current.surfaceVariant,
            checkedBorderColor = Color.Transparent,
            uncheckedBorderColor = Color.Transparent
        )
    )
}

// 3. GKBadge
enum class BadgeStyle { CRITICAL, HIGH, MEDIUM, LOW, ALLOWED, BLOCKED }

@Composable
fun GKBadge(text: String, style: BadgeStyle) {
    val (bgColor, textColor, borderStroke) = when (style) {
        BadgeStyle.CRITICAL -> Triple(LocalGKColors.current.accentRed, Color.White, null)
        BadgeStyle.HIGH -> Triple(LocalGKColors.current.accentOrange, Color.White, null)
        BadgeStyle.MEDIUM -> Triple(Color.Transparent, LocalGKColors.current.accentOrange, androidx.compose.foundation.BorderStroke(1.dp, LocalGKColors.current.accentOrange))
        BadgeStyle.LOW -> Triple(Color.Transparent, LocalGKColors.current.textTertiary, androidx.compose.foundation.BorderStroke(1.dp, LocalGKColors.current.textTertiary))
        BadgeStyle.ALLOWED -> Triple(LocalGKColors.current.accentGreen, LocalGKColors.current.textOnPrimary, null)
        BadgeStyle.BLOCKED -> Triple(LocalGKColors.current.accentRed, Color.White, null)
    }
    
    var modifier = Modifier.clip(RoundedCornerShape(99.dp))
    if (borderStroke != null) {
        modifier = modifier.border(borderStroke, RoundedCornerShape(99.dp))
    }
    modifier = modifier.background(bgColor).padding(horizontal = 8.dp, vertical = 4.dp)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// 4. GKSectionHeader
@Composable
fun GKSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        color = LocalGKColors.current.textTertiary,
        letterSpacing = 0.4.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

// 5. GKPrimaryButton / GKOutlineButton
@Composable
fun GKPrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LocalGKColors.current.primary,
            contentColor = LocalGKColors.current.textOnPrimary,
            disabledContainerColor = LocalGKColors.current.surfaceVariant,
            disabledContentColor = LocalGKColors.current.textTertiary
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GKOutlineButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, LocalGKColors.current.border),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = LocalGKColors.current.textPrimary
        )
    ) {
        Text(text)
    }
}

// 6. GKEmptyState
@Composable
fun GKEmptyState(
    icon: ImageVector, title: String, subtitle: String,
    primaryActionText: String? = null, onPrimaryAction: (() -> Unit)? = null,
    secondaryActionText: String? = null, onSecondaryAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = LocalGKColors.current.textTertiary, modifier = Modifier.size(52.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = LocalGKColors.current.textSecondary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LocalGKColors.current.textTertiary, textAlign = TextAlign.Center)
        
        if (primaryActionText != null && onPrimaryAction != null) {
            Spacer(modifier = Modifier.height(20.dp))
            GKPrimaryButton(primaryActionText, onClick = onPrimaryAction)
        }
        if (secondaryActionText != null && onSecondaryAction != null) {
            Spacer(modifier = Modifier.height(8.dp))
            GKOutlineButton(secondaryActionText, onClick = onSecondaryAction)
        }
    }
}

// 7. GKFilterChips
@Composable
fun GKFilterChips(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(0.5.dp, LocalGKColors.current.border, RoundedCornerShape(8.dp))
            .background(LocalGKColors.current.surfaceVariant)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) LocalGKColors.current.primary else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) LocalGKColors.current.textOnPrimary else LocalGKColors.current.textPrimary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// 8. GKModuleHeader — standard header card used by ALL 6 module screens
/**
 * Glassmorphic header card for module screens.
 * Shows: colored left stripe, icon in color container, title, subtitle, master toggle.
 * Used identically by App Gate, Web Gate, Threat Intel, Wi-Fi Guard, Trust Check, Privacy Scan.
 */
@Composable
fun GKModuleHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    moduleColor: Color,
    isEnabled: Boolean = true,
    onToggle: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(moduleColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = moduleColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LocalGKColors.current.textPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalGKColors.current.textSecondary
                )
            }
            if (onToggle != null) {
                Spacer(Modifier.width(12.dp))
                GKToggle(checked = isEnabled, onCheckedChange = onToggle)
            }
        }
    }
}

// ─── Info Dialog & Button ─────────────────────────────────────────────────────

/**
 * Tappable ℹ️ icon that the user can tap to learn what a feature does.
 * Pair with [GKInfoDialog] to show a plain-English explanation.
 */
@Composable
fun GKInfoButton(
    color: Color = LocalGKColors.current.primary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "What is this?",
            tint = color.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Plain-English feature explanation dialog.
 * Show this when the user taps a [GKInfoButton].
 *
 * @param title   Short feature name, e.g. "App Gate"
 * @param body    Layman-friendly explanation of what the feature does
 * @param onDismiss Called when the user dismisses the dialog
 */
@Composable
fun GKInfoDialog(
    title: String,
    body: String,
    accentColor: Color = LocalGKColors.current.primary,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalGKColors.current.surface,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Info,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = LocalGKColors.current.textPrimary,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalGKColors.current.textSecondary,
                textAlign = TextAlign.Start,
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = accentColor, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
