package com.example.servicehub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Shared building blocks so every Home screen has the SAME look:
 *  - SectionLabel: small grey title above each section (with optional "See all")
 *  - ActionList + ActionRow: one white card, one row per thing to do
 *    (icon on the left, text in the middle, action on the right)
 *  - SoftIcon: a small icon on a light rounded square of its own colour
 *
 * Colour meaning used everywhere:
 *   orange = money / waiting for you,  blue = booking info,  green = done / paid,  red = problem
 */

/** Small grey section title, e.g. "NEEDS YOUR ATTENTION", with an optional "See all" on the right. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, actionText: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionText, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

/** A small icon on a light, rounded square of the same colour (looks calmer than big circles). */
@Composable
fun SoftIcon(icon: ImageVector, color: Color, size: Dp = 36.dp, iconSize: Dp = 20.dp) {
    Box(
        Modifier.size(size).background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize)) }
}

/** One item in an [ActionList]. [action] is the short word on the right, e.g. "Pay", "Rate", "View". */
data class ActionItem(
    val icon: ImageVector,
    val color: Color,
    val title: String,
    val subtitle: String,
    val action: String,
    val onClick: () -> Unit,
)

/** One white card with a row per item and thin lines between them. Shows nothing if empty. */
@Composable
fun ActionList(items: List<ActionItem>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    AppCard(modifier) {
        items.forEachIndexed { index, item ->
            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ActionRow(item)
        }
    }
}

@Composable
fun ActionRow(item: ActionItem) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = item.onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoftIcon(item.icon, item.color)
        Column(Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.subtitle.isNotBlank()) {
                Text(
                    item.subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(item.action, style = MaterialTheme.typography.labelLarge, color = item.color, fontWeight = FontWeight.SemiBold)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp),
        )
    }
}
