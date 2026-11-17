package com.example.servicehub.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
 * Small building blocks used on every screen, so the whole app looks consistent:
 * white cards with a thin border, icons in soft tinted squares, clear section titles,
 * a fixed action bar at the bottom of detail screens.
 */

/** White card with rounded corners and a thin border. Clickable if [onClick] is given. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(clickable.padding(16.dp), content = content)
    }
}

/** An icon inside a softly tinted rounded square (instead of emoji). */
@Composable
fun IconTile(icon: ImageVector, color: Color, size: Dp = 44.dp, iconSize: Dp = 22.dp) {
    Box(
        Modifier.size(size).background(color.copy(alpha = 0.12f), RoundedCornerShape(size / 3.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(iconSize))
    }
}

/** Section title with an optional link on the right, e.g. "Recent reviews      See all". */
@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * A row in a list: icon tile, title, subtitle, and an arrow (or custom [trailing]).
 * Used for settings-style lists (profile, admin shortcuts).
 */
@Composable
fun ListRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    color: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null,
) {
    val clickable = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        clickable.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, color, size = 40.dp, iconSize = 20.dp)
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) {
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A number with a label and an icon, e.g. "Total bookings 24". */
@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    AppCard(modifier = modifier, onClick = onClick) {
        IconTile(icon, color, size = 36.dp, iconSize = 18.dp)
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 10.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * The fixed bar at the bottom of a detail screen with the main action(s),
 * e.g. "Total ₹198   [Confirm booking]". Thin line on top, white background.
 */
@Composable
fun BottomActionBar(content: @Composable RowScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

/** Small grey caption text, e.g. under a section title. */
@Composable
fun Caption(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = color, modifier = modifier)
}

/** A tinted information box, e.g. "Ramesh is off today (Tuesday)". */
@Composable
fun InfoBanner(icon: ImageVector, text: String, color: Color, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f), MaterialTheme.shapes.small)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 10.dp))
        action?.invoke()
    }
}

/** Five star icons, filled up to [rating] (1–5). */
@Composable
fun StarRow(rating: Int, size: Dp = 14.dp) {
    val stars = rating.coerceIn(0, 5)   // safety: never more than 5 or less than 0
    Row {
        repeat(5) { index ->
            Icon(
                if (index < stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = Color(0xFFF5A623),
                modifier = Modifier.size(size),
            )
        }
    }
}

/**
 * Vertical bars, one per item (e.g. 7 days). Height is relative to the biggest value.
 * Built from plain Boxes: no chart library needed. The last bar is highlighted ("today").
 */
@Composable
fun MiniBarChart(items: List<Pair<String, Int>>, color: Color, height: Dp = 90.dp) {
    val max = items.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        items.forEachIndexed { index, (label, value) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (value > 0) Text("$value", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(
                    Modifier
                        .padding(top = 2.dp)
                        .size(width = 16.dp, height = (height.value * value / max).dp.coerceAtLeast(3.dp))
                        .background(
                            if (index == items.lastIndex) color else color.copy(alpha = if (value > 0) 0.45f else 0.15f),
                            RoundedCornerShape(4.dp),
                        )
                )
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Horizontal bars with a label and value, e.g. revenue per category. */
@Composable
fun HorizontalBars(items: List<Pair<String, Int>>, color: Color, format: (Int) -> String) {
    val max = items.maxOfOrNull { it.second }?.takeIf { it > 0 } ?: 1
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { (label, value) ->
            Column {
                Row {
                    Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Text(format(value), style = MaterialTheme.typography.labelMedium)
                }
                Box(Modifier.padding(top = 4.dp).fillMaxWidth().background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))) {
                    Box(
                        Modifier
                            .fillMaxWidth(value.toFloat() / max)
                            .height(8.dp)
                            .background(color, RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}

/** Bold label used inside cards. */
@Composable
fun CardTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}
