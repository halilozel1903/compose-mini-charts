package io.github.halilozel1903.minicharts

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.minicharts.core.ChartData
import io.github.halilozel1903.minicharts.core.ChartSummary
import io.github.halilozel1903.minicharts.core.LineCurve
import io.github.halilozel1903.minicharts.core.LineGeometry
import io.github.halilozel1903.minicharts.core.PlotArea
import io.github.halilozel1903.minicharts.core.ValueFormatter
import io.github.halilozel1903.minicharts.core.ValueRange
import kotlin.math.max

/**
 * A small line without axes, for lists, cards and KPI tiles. The last point gets a dot, and
 * [highlightMinMax] marks the lowest and highest points too.
 *
 * @param title What the line shows; starts the accessibility summary ("Balance, 30 points, from ...").
 * @param labels Optional labels of the points, for the summary ("highest in July").
 */
@Composable
public fun Sparkline(
    values: List<Number>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp,
    fill: Boolean = true,
    curve: LineCurve = LineCurve.Smooth,
    showLastPoint: Boolean = true,
    highlightMinMax: Boolean = false,
    minMaxColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    title: String = "Trend",
    labels: List<String> = emptyList(),
    valueFormatter: ValueFormatter = ValueFormatter.Compact,
    animationSpec: AnimationSpec<Float>? = ChartDefaults.EntryAnimation,
    contentDescription: String? = null,
) {
    val data = remember(values) { ChartData.sanitize(values) }
    val progress = rememberEntryProgress(data, animationSpec)
    val summary = contentDescription ?: remember(data, title, labels, valueFormatter) {
        ChartSummary.line(title, data, labels, valueFormatter)
    }
    Canvas(
        modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 20.dp)
            .semantics { this.contentDescription = summary },
    ) {
        if (data.isEmpty()) return@Canvas
        val range = ValueRange.of(data) ?: return@Canvas
        val dot = strokeWidth.toPx() * 1.6f
        val inset = max(strokeWidth.toPx(), dot) + 1f
        val area = PlotArea(inset, inset, size.width - inset, size.height - inset)
        if (area.width <= 0f || area.height <= 0f) return@Canvas
        val flat = range.span == 0.0
        val yMin = if (flat) range.min - 1.0 else range.min
        val yMax = if (flat) range.max + 1.0 else range.max
        val points = LineGeometry.points(data, area, yMin, yMax)
        val segments = LineGeometry.segments(points, curve)
        clipRect(right = area.left + (size.width - area.left) * progress.value) {
            if (fill && points.size > 1) {
                drawPath(
                    segments.toAreaPath(size.height),
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.24f), color.copy(alpha = 0f)),
                        startY = area.top,
                        endY = size.height,
                    ),
                )
            }
            if (points.size > 1) {
                drawPath(
                    segments.toPath(),
                    color = color,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            if (highlightMinMax && !flat) {
                val low = points[data.indexOf(range.min)]
                val high = points[data.indexOf(range.max)]
                drawCircle(minMaxColor, radius = dot * 0.8f, center = Offset(low.x, low.y))
                drawCircle(minMaxColor, radius = dot * 0.8f, center = Offset(high.x, high.y))
            }
            if (showLastPoint || points.size == 1) {
                val last = points.last()
                drawCircle(color, radius = dot, center = Offset(last.x, last.y))
            }
        }
    }
}
