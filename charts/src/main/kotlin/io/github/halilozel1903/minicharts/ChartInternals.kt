package io.github.halilozel1903.minicharts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.minicharts.core.PathSegment
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Entry animation progress from 0 to 1, restarted when [key] changes. Without an [animationSpec], or when
 * [LocalChartAnimationsEnabled] is false, it is 1 from the first frame.
 */
@Composable
internal fun rememberEntryProgress(key: Any?, animationSpec: AnimationSpec<Float>?): State<Float> {
    val spec = animationSpec?.takeIf { LocalChartAnimationsEnabled.current }
    val progress = remember(key) { Animatable(if (spec == null) 1f else 0f) }
    LaunchedEffect(progress, spec) {
        if (spec == null) progress.snapTo(1f) else progress.animateTo(1f, spec)
    }
    return progress.asState()
}

/** The current pixel geometry, written while drawing and read by touch handlers (which always come later). */
internal class HitCache {
    var xs: List<Float> = emptyList()
    var slots: List<io.github.halilozel1903.minicharts.core.Span> = emptyList()
    var arcs: List<io.github.halilozel1903.minicharts.core.DonutArc> = emptyList()
    var center: Offset = Offset.Zero
    var innerRadius: Float = 0f
    var outerRadius: Float = 0f
}

internal fun List<PathSegment>.toPath(): Path {
    val path = Path()
    for (segment in this) {
        when (segment) {
            is PathSegment.MoveTo -> path.moveTo(segment.end.x, segment.end.y)
            is PathSegment.LineTo -> path.lineTo(segment.end.x, segment.end.y)
            is PathSegment.CubicTo -> path.cubicTo(
                segment.control1.x, segment.control1.y,
                segment.control2.x, segment.control2.y,
                segment.end.x, segment.end.y,
            )
        }
    }
    return path
}

/** The line path closed down to [baseline], for area fills. */
internal fun List<PathSegment>.toAreaPath(baseline: Float): Path {
    val path = toPath()
    if (isEmpty()) return path
    path.lineTo(last().end.x, baseline)
    path.lineTo(first().end.x, baseline)
    path.close()
    return path
}

/** A bar with rounded corners only at its value end (top for positive, bottom for negative values). */
internal fun DrawScope.drawBar(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    color: Color,
    radius: Float,
    roundTop: Boolean,
    roundBottom: Boolean,
) {
    val width = right - left
    val height = bottom - top
    if (width <= 0f || height <= 0f) return
    val r = min(radius, min(width / 2f, height))
    if (r < 0.5f || (!roundTop && !roundBottom)) {
        drawRect(color, topLeft = Offset(left, top), size = Size(width, height))
        return
    }
    val top0 = if (roundTop) CornerRadius(r) else CornerRadius.Zero
    val bottom0 = if (roundBottom) CornerRadius(r) else CornerRadius.Zero
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                left = left,
                top = top,
                right = right,
                bottom = bottom,
                topLeftCornerRadius = top0,
                topRightCornerRadius = top0,
                bottomRightCornerRadius = bottom0,
                bottomLeftCornerRadius = bottom0,
            ),
        )
    }
    drawPath(path, color)
}

/** One row of a tooltip: a colored dot (when [color] is set) and text. */
internal class TooltipRow(val text: String, val color: Color?)

/**
 * Draws a tooltip next to ([anchorX], [anchorY]): on the right of the anchor when it fits, otherwise on the
 * left, and always inside [bounds].
 */
internal fun DrawScope.drawTooltip(
    measurer: TextMeasurer,
    title: String?,
    rows: List<TooltipRow>,
    anchorX: Float,
    anchorY: Float,
    bounds: Rect,
    container: Color,
    titleStyle: TextStyle,
    bodyStyle: TextStyle,
) {
    val padding = 10.dp.toPx()
    val dot = 8.dp.toPx()
    val dotGap = 6.dp.toPx()
    val rowGap = 2.dp.toPx()
    val distance = 12.dp.toPx()
    val titleLayout = title?.let { measurer.measure(it, style = titleStyle, maxLines = 1) }
    val rowLayouts: List<TextLayoutResult> = rows.map { measurer.measure(it.text, style = bodyStyle, maxLines = 1) }
    var contentWidth = titleLayout?.size?.width?.toFloat() ?: 0f
    rows.forEachIndexed { index, row ->
        val indent = if (row.color != null) dot + dotGap else 0f
        contentWidth = max(contentWidth, indent + rowLayouts[index].size.width)
    }
    var contentHeight = titleLayout?.size?.height?.toFloat() ?: 0f
    rowLayouts.forEach { contentHeight += it.size.height + rowGap }
    if (titleLayout == null && rowLayouts.isNotEmpty()) contentHeight -= rowGap
    val width = ceil(contentWidth + padding * 2)
    val height = ceil(contentHeight + padding * 2)

    var x = anchorX + distance
    if (x + width > bounds.right) x = anchorX - distance - width
    x = x.coerceIn(bounds.left, max(bounds.left, bounds.right - width))
    val y = (anchorY - height / 2f).coerceIn(bounds.top, max(bounds.top, bounds.bottom - height))

    drawRoundRect(container, topLeft = Offset(x, y), size = Size(width, height), cornerRadius = CornerRadius(8.dp.toPx()))
    var cursor = y + padding
    if (titleLayout != null) {
        drawText(titleLayout, topLeft = Offset(x + padding, cursor))
        cursor += titleLayout.size.height + rowGap
    }
    rows.forEachIndexed { index, row ->
        val layout = rowLayouts[index]
        var textX = x + padding
        if (row.color != null) {
            drawCircle(row.color, radius = dot / 2f, center = Offset(textX + dot / 2f, cursor + layout.size.height / 2f))
            textX += dot + dotGap
        }
        drawText(layout, topLeft = Offset(textX, cursor))
        cursor += layout.size.height + rowGap
    }
}

/**
 * Category labels under the x axis, centred on [xs]. Every n-th label is drawn so neighbours keep at least
 * [minGap] pixels between them; the first and last stay inside the canvas.
 */
internal fun DrawScope.drawXLabels(
    measurer: TextMeasurer,
    labels: List<String>,
    xs: List<Float>,
    top: Float,
    style: TextStyle,
    minGap: Float,
) {
    val count = min(labels.size, xs.size)
    if (count == 0) return
    val layouts = (0 until count).map { measurer.measure(labels[it], style = style, maxLines = 1) }
    val widest = layouts.maxOf { it.size.width }.toFloat()
    val spacing = if (count > 1) (xs[count - 1] - xs[0]) / (count - 1) else Float.MAX_VALUE
    val every = if (spacing <= 0f) count else max(1, ceil((widest + minGap) / spacing).toInt())
    for (index in 0 until count step every) {
        val layout = layouts[index]
        val left = (xs[index] - layout.size.width / 2f).coerceIn(0f, max(0f, size.width - layout.size.width))
        drawText(layout, topLeft = Offset(left, top))
    }
}
