package io.github.halilozel1903.minicharts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

/** One entry of a [ChartLegend]: a color dot, a label and an optional value such as "42%". */
@Immutable
public data class LegendItem(
    val label: String,
    val color: Color,
    val value: String? = null,
)

/**
 * A legend for any chart. Horizontal legends wrap onto more lines and are centred; vertical legends put each
 * item on its own line with the value at the end. With [onItemClick] items are clickable, and
 * [selectedIndex] is shown in bold.
 */
@Composable
public fun ChartLegend(
    items: List<LegendItem>,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    selectedIndex: Int? = null,
    onItemClick: ((Int) -> Unit)? = null,
) {
    if (vertical) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items.forEachIndexed { index, item ->
                LegendEntry(item, index == selectedIndex, onItemClick?.let { click -> { click(index) } }, Modifier.fillMaxWidth(), fillWidth = true)
            }
        }
    } else {
        CenteredFlow(horizontalSpacing = 12.dp, verticalSpacing = 4.dp, modifier = modifier) {
            items.forEachIndexed { index, item ->
                LegendEntry(item, index == selectedIndex, onItemClick?.let { click -> { click(index) } }, Modifier, fillWidth = false)
            }
        }
    }
}

@Composable
private fun LegendEntry(
    item: LegendItem,
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier,
    fillWidth: Boolean,
) {
    val clickable = if (onClick != null) {
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    Row(
        modifier
            .then(clickable)
            .semantics { this.selected = selected }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(if (selected) 12.dp else 10.dp)
                .clip(CircleShape)
                .background(item.color),
        )
        Spacer(Modifier.width(8.dp))
        val weight = if (selected) FontWeight.Bold else FontWeight.Normal
        Text(
            text = item.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = weight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (fillWidth) Modifier.weight(1f) else Modifier,
        )
        if (item.value != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = item.value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

/** Places children in rows like text, wrapping when a row is full, each row centred. */
@Composable
private fun CenteredFlow(
    horizontalSpacing: Dp,
    verticalSpacing: Dp,
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val hGap = horizontalSpacing.roundToPx()
        val vGap = verticalSpacing.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val rows = ArrayList<MutableList<Int>>()
        var rowWidth = 0
        placeables.forEachIndexed { index, placeable ->
            if (rows.isEmpty() || rowWidth + hGap + placeable.width > maxWidth) {
                rows.add(mutableListOf(index))
                rowWidth = placeable.width
            } else {
                rows.last().add(index)
                rowWidth += hGap + placeable.width
            }
        }
        val widths = rows.map { row -> row.sumOf { placeables[it].width } + hGap * (row.size - 1) }
        val heights = rows.map { row -> row.maxOf { placeables[it].height } }
        val contentWidth = widths.maxOrNull() ?: 0
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else contentWidth
        val height = heights.sum() + vGap * max(0, rows.size - 1)
        layout(
            width.coerceIn(constraints.minWidth, constraints.maxWidth),
            height.coerceIn(constraints.minHeight, constraints.maxHeight),
        ) {
            var y = 0
            rows.forEachIndexed { r, row ->
                var x = max(0, (width - widths[r]) / 2)
                row.forEach { index ->
                    val placeable = placeables[index]
                    placeable.placeRelative(x, y + (heights[r] - placeable.height) / 2)
                    x += placeable.width + hGap
                }
                y += heights[r] + vGap
            }
        }
    }
}
