package com.gatekeeper.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
        BadgeStyle.CRITICAL -> Triple(AccentRed, Color.White, null)
        BadgeStyle.HIGH -> Triple(AccentOrange, Color.White, null)
        BadgeStyle.MEDIUM -> Triple(Color.Transparent, AccentOrange, androidx.compose.foundation.BorderStroke(1.dp, AccentOrange))
        BadgeStyle.LOW -> Triple(Color.Transparent, LocalGKColors.current.textTertiary, androidx.compose.foundation.BorderStroke(1.dp, LocalGKColors.current.textTertiary))
        BadgeStyle.ALLOWED -> Triple(AccentGreen, LocalGKColors.current.textOnPrimary, null)
        BadgeStyle.BLOCKED -> Triple(AccentRed, Color.White, null)
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
