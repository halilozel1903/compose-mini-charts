package io.github.halilozel1903.minicharts

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.minicharts.core.ChartData
import io.github.halilozel1903.minicharts.core.ChartSummary
import io.github.halilozel1903.minicharts.core.HitTest
import io.github.halilozel1903.minicharts.core.LineCurve
import io.github.halilozel1903.minicharts.core.LineGeometry
import io.github.halilozel1903.minicharts.core.LinearScale
import io.github.halilozel1903.minicharts.core.NiceScale
import io.github.halilozel1903.minicharts.core.PlotArea
import io.github.halilozel1903.minicharts.core.SeriesData
import io.github.halilozel1903.minicharts.core.ValueFormatter
import io.github.halilozel1903.minicharts.core.ValueRange
import kotlin.math.max

/**
 * One line of a [LineChart]. [values] can be any numbers (`Int`, `Float`, `Double`...).
 * An unspecified [color] takes the next series color of [ChartColors].
 */
@Immutable
public data class LineSeries(
    val name: String,
    val values: List<Number>,
    val color: Color = Color.Unspecified,
    /** A gradient under the line, fading towards the bottom. */
    val fill: Boolean = true,
    val curve: LineCurve = LineCurve.Smooth,
    val strokeWidth: Dp = 2.5.dp,
    /** A dashed line, for example for a forecast or last year's data. */
    val dashed: Boolean = false,
)

/**
 * A line chart with one or more series, an optional area fill, grid lines, axis labels and a touch highlight:
 * tap or drag across the chart to see the values at a position in a tooltip. Tap the highlighted position again
 * to hide it.
 *
 * Give it a height, for example `Modifier.fillMaxWidth().height(220.dp)`.
 *
 * @param labels Category labels under the x axis, one per point (months, days...). Also used in the tooltip
 *   and in the accessibility summary.
 * @param title What the chart shows; starts the accessibility summary ("Revenue, 12 points, ...").
 * @param includeZero Whether the y axis must start at (or contain) zero.
 * @param initialHighlight The index highlighted when the chart first appears, or null.
 * @param onHighlightChange Called with the highlighted index, or null when the highlight is cleared.
 * @param animationSpec The entry animation; null shows the final chart at once.
 * @param contentDescription Overrides the generated accessibility summary.
 */
@Composable
public fun LineChart(
    series: List<LineSeries>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    title: String = "Line chart",
    valueFormatter: ValueFormatter = ValueFormatter.Compact,
    colors: ChartColors = ChartDefaults.colors(),
    showGrid: Boolean = true,
    showYAxis: Boolean = true,
    showXAxis: Boolean = true,
    yTickCount: Int = 4,
    includeZero: Boolean = false,
    showPoints: Boolean = false,
    showLegend: Boolean = series.size > 1,
    highlightEnabled: Boolean = true,
    initialHighlight: Int? = null,
    onHighlightChange: ((Int?) -> Unit)? = null,
    animationSpec: AnimationSpec<Float>? = ChartDefaults.EntryAnimation,
    contentDescription: String? = null,
) {
    val data = remember(series) { series.map { ChartData.sanitize(it.values) } }
    val slots = data.maxOfOrNull { it.size } ?: 0
    val lineColors = series.mapIndexed { index, item -> colors.resolve(item.color, index) }
    val measurer = rememberTextMeasurer()
    val axisStyle = ChartDefaults.axisLabelStyle().copy(color = colors.axisLabel)
    val tooltipBody = ChartDefaults.tooltipTextStyle().copy(color = colors.tooltipContent)
    val tooltipTitle = tooltipBody.copy(fontWeight = FontWeight.SemiBold)
    val progress = rememberEntryProgress(data, animationSpec)
    val cache = remember { HitCache() }

    var highlighted by rememberSaveable { mutableStateOf<Int?>(initialHighlight) }
    val current = highlighted?.takeIf { it in 0 until slots }
    val onChange by rememberUpdatedState(onHighlightChange)
    val select: (Int?) -> Unit = { index ->
        if (index != highlighted) {
            highlighted = index
            onChange?.invoke(index)
        }
    }
    val selectState = rememberUpdatedState(select)

    val summary = contentDescription ?: remember(data, labels, title, valueFormatter) {
        ChartSummary.lines(title, series.mapIndexed { index, item -> SeriesData(item.name, data[index]) }, labels, valueFormatter)
    }
    val stateText = current?.let { index ->
        val where = labels.getOrNull(index) ?: "Point ${index + 1}"
        where + ": " + series.indices.filter { index < data[it].size }.joinToString(", ") { s ->
            val value = valueFormatter.format(data[s][index])
            if (series.size == 1) value else "${series[s].name} $value"
        }
    }

    val touch = if (highlightEnabled && slots > 0) {
        Modifier
            .pointerInput(slots) {
                detectTapGestures { offset ->
                    val index = HitTest.nearestIndex(cache.xs, offset.x).takeIf { it >= 0 }
                    selectState.value(if (index == highlighted) null else index)
                }
            }
            .pointerInput(slots) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        selectState.value(HitTest.nearestIndex(cache.xs, offset.x).takeIf { it >= 0 })
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        selectState.value(HitTest.nearestIndex(cache.xs, change.position.x).takeIf { it >= 0 })
                    },
                )
            }
    } else {
        Modifier
    }

    Column(modifier.defaultMinSize(minHeight = 120.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .semantics {
                    this.contentDescription = summary
                    if (stateText != null) stateDescription = stateText
                }
                .then(touch),
        ) {
            if (slots == 0) return@Canvas
            val range = ValueRange.of(data.flatten()) ?: return@Canvas
            val ticks = NiceScale.ticks(range.min, range.max, max(2, yTickCount), includeZero)
            val gap = 6.dp.toPx()
            val tickLayouts = if (showYAxis) {
                ticks.values.map { measurer.measure(valueFormatter.format(it), style = axisStyle, maxLines = 1) }
            } else {
                emptyList()
            }
            val labelHeight = measurer.measure("0", style = axisStyle).size.height.toFloat()
            val strokeMax = series.maxOfOrNull { it.strokeWidth.toPx() } ?: 0f
            val edge = max(6.dp.toPx(), strokeMax)
            val area = PlotArea(
                left = if (showYAxis) (tickLayouts.maxOfOrNull { it.size.width } ?: 0) + gap else edge,
                top = if (showYAxis) max(edge, labelHeight / 2f) else edge,
                right = size.width - edge,
                bottom = size.height - (if (showXAxis && labels.isNotEmpty()) labelHeight + gap else edge),
            )
            if (area.width <= 0f || area.height <= 0f) return@Canvas
            val scale = LinearScale.vertical(ticks.min, ticks.max, area)
            val xs = LineGeometry.xPositions(slots, area)
            cache.xs = xs

            ticks.values.forEachIndexed { index, tick ->
                val y = scale.map(tick)
                if (showGrid) {
                    drawLine(colors.grid, Offset(area.left, y), Offset(area.right, y), strokeWidth = 1.dp.toPx())
                }
                if (showYAxis) {
                    val layout = tickLayouts[index]
                    drawText(layout, topLeft = Offset(area.left - gap - layout.size.width, y - layout.size.height / 2f))
                }
            }
            if (showXAxis && labels.isNotEmpty()) {
                drawXLabels(measurer, labels, xs, area.bottom + gap, axisStyle, minGap = 8.dp.toPx())
            }

            val points = data.map { values -> LineGeometry.points(values, area, ticks.min, ticks.max, slots) }
            val p = progress.value
            clipRect(right = area.left + (area.right + edge - area.left) * p) {
                series.forEachIndexed { s, item ->
                    val pts = points[s]
                    if (pts.isEmpty()) return@forEachIndexed
                    val color = lineColors[s]
                    val segments = LineGeometry.segments(pts, item.curve)
                    if (item.fill && pts.size > 1) {
                        drawPath(
                            segments.toAreaPath(area.bottom),
                            brush = Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f)),
                                startY = area.top,
                                endY = area.bottom,
                            ),
                        )
                    }
                    if (pts.size > 1) {
                        drawPath(
                            segments.toPath(),
                            color = color,
                            style = Stroke(
                                width = item.strokeWidth.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = if (item.dashed) {
                                    PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
                                } else {
                                    null
                                },
                            ),
                        )
                    }
                    if (showPoints || pts.size == 1) {
                        pts.forEach { point -> drawCircle(color, radius = item.strokeWidth.toPx() * 1.4f, center = Offset(point.x, point.y)) }
                    }
                }
            }

            if (current != null && p >= 1f) {
                val x = xs[current]
                drawLine(colors.highlight, Offset(x, area.top), Offset(x, area.bottom), strokeWidth = 1.dp.toPx())
                var topY = area.bottom
                val rows = ArrayList<TooltipRow>()
                series.forEachIndexed { s, item ->
                    val point = points[s].getOrNull(current) ?: return@forEachIndexed
                    topY = minOf(topY, point.y)
                    drawCircle(lineColors[s], radius = 6.dp.toPx(), center = Offset(point.x, point.y))
                    drawCircle(colors.background, radius = 3.dp.toPx(), center = Offset(point.x, point.y))
                    val value = valueFormatter.format(data[s][current])
                    rows += if (series.size == 1) TooltipRow(value, null) else TooltipRow("${item.name}  $value", lineColors[s])
                }
                drawTooltip(
                    measurer = measurer,
                    title = labels.getOrNull(current),
                    rows = rows,
                    anchorX = x,
                    anchorY = topY,
                    bounds = Rect(0f, 0f, size.width, size.height),
                    container = colors.tooltipContainer,
                    titleStyle = tooltipTitle,
                    bodyStyle = tooltipBody,
                )
            }
        }
        if (showLegend && series.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            ChartLegend(
                items = series.mapIndexed { index, item -> LegendItem(item.name, lineColors[index]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * A line chart of a single series: `LineChart(values = revenue, labels = months, title = "Revenue")`.
 * See the other overload for every parameter.
 */
@Composable
public fun LineChart(
    values: List<Number>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    title: String = "Line chart",
    color: Color = MaterialTheme.colorScheme.primary,
    fill: Boolean = true,
    curve: LineCurve = LineCurve.Smooth,
    valueFormatter: ValueFormatter = ValueFormatter.Compact,
    colors: ChartColors = ChartDefaults.colors(),
    showGrid: Boolean = true,
    showYAxis: Boolean = true,
    showXAxis: Boolean = true,
    includeZero: Boolean = false,
    initialHighlight: Int? = null,
    onHighlightChange: ((Int?) -> Unit)? = null,
    animationSpec: AnimationSpec<Float>? = ChartDefaults.EntryAnimation,
    contentDescription: String? = null,
) {
    LineChart(
        series = listOf(LineSeries(title, values, color, fill, curve)),
        modifier = modifier,
        labels = labels,
        title = title,
        valueFormatter = valueFormatter,
        colors = colors,
        showGrid = showGrid,
        showYAxis = showYAxis,
        showXAxis = showXAxis,
        includeZero = includeZero,
        showLegend = false,
        initialHighlight = initialHighlight,
        onHighlightChange = onHighlightChange,
        animationSpec = animationSpec,
        contentDescription = contentDescription,
    )
}
