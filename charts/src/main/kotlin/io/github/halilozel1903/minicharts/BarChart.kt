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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.minicharts.core.BarGeometry
import io.github.halilozel1903.minicharts.core.BarMode
import io.github.halilozel1903.minicharts.core.ChartSummary
import io.github.halilozel1903.minicharts.core.HitTest
import io.github.halilozel1903.minicharts.core.LinearScale
import io.github.halilozel1903.minicharts.core.NiceScale
import io.github.halilozel1903.minicharts.core.PlotArea
import io.github.halilozel1903.minicharts.core.ValueFormatter
import kotlin.math.max

/**
 * One data set of a [BarChart]: one value per category (group). An unspecified [color] takes the next
 * series color of [ChartColors].
 */
@Immutable
public data class BarSeries(
    val name: String,
    val values: List<Number>,
    val color: Color = Color.Unspecified,
)

/**
 * A bar chart with one or more series, side by side ([BarMode.Grouped]) or stacked ([BarMode.Stacked]),
 * negative values included. Tap or drag across the chart to highlight a category and see its values in a
 * tooltip; tap it again to hide it.
 *
 * Give it a height, for example `Modifier.fillMaxWidth().height(220.dp)`.
 *
 * @param labels Category labels under the bars (days, months...), also used in the tooltip and summary.
 * @param title What the chart shows; starts the accessibility summary.
 * @param groupSpacing The part of each category's slot (0..1) left empty between categories.
 * @param barSpacing Space between the bars of one category in [BarMode.Grouped].
 * @param stackSpacing Space between the segments of a stack in [BarMode.Stacked].
 * @param cornerRadius Rounding of the value end of each bar (the outer end of a stack).
 * @param initialHighlight The category highlighted when the chart first appears, or null.
 * @param animationSpec The entry animation (bars grow from zero); null shows the final chart at once.
 */
@Composable
public fun BarChart(
    series: List<BarSeries>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    title: String = "Bar chart",
    mode: BarMode = BarMode.Grouped,
    valueFormatter: ValueFormatter = ValueFormatter.Compact,
    colors: ChartColors = ChartDefaults.colors(),
    showGrid: Boolean = true,
    showYAxis: Boolean = true,
    showXAxis: Boolean = true,
    yTickCount: Int = 4,
    groupSpacing: Float = 0.35f,
    barSpacing: Dp = 4.dp,
    stackSpacing: Dp = 2.dp,
    cornerRadius: Dp = 6.dp,
    showLegend: Boolean = series.size > 1,
    highlightEnabled: Boolean = true,
    initialHighlight: Int? = null,
    onHighlightChange: ((Int?) -> Unit)? = null,
    animationSpec: AnimationSpec<Float>? = ChartDefaults.EntryAnimation,
    contentDescription: String? = null,
) {
    // data[group][series]
    val data: List<List<Double>> = remember(series) {
        val groups = series.maxOfOrNull { it.values.size } ?: 0
        List(groups) { g ->
            series.map { item ->
                val value = item.values.getOrNull(g)?.toDouble() ?: 0.0
                if (value.isFinite()) value else 0.0
            }
        }
    }
    val groups = data.size
    val barColors = series.mapIndexed { index, item -> colors.resolve(item.color, index) }
    val measurer = rememberTextMeasurer()
    val axisStyle = ChartDefaults.axisLabelStyle().copy(color = colors.axisLabel)
    val tooltipBody = ChartDefaults.tooltipTextStyle().copy(color = colors.tooltipContent)
    val tooltipTitle = tooltipBody.copy(fontWeight = FontWeight.SemiBold)
    val progress = rememberEntryProgress(data, animationSpec)
    val cache = remember { HitCache() }

    var highlighted by rememberSaveable { mutableStateOf<Int?>(initialHighlight) }
    val current = highlighted?.takeIf { it in 0 until groups }
    val onChange by rememberUpdatedState(onHighlightChange)
    val select: (Int?) -> Unit = { index ->
        if (index != highlighted) {
            highlighted = index
            onChange?.invoke(index)
        }
    }
    val selectState = rememberUpdatedState(select)

    val summary = contentDescription ?: remember(data, labels, title, mode, valueFormatter) {
        ChartSummary.bars(title, data, labels, series.map { it.name }, mode, valueFormatter)
    }
    val stateText = current?.let { g ->
        val where = labels.getOrNull(g) ?: "Bar ${g + 1}"
        where + ": " + series.indices.joinToString(", ") { s ->
            val value = valueFormatter.format(data[g][s])
            if (series.size == 1) value else "${series[s].name} $value"
        }
    }

    val touch = if (highlightEnabled && groups > 0) {
        Modifier
            .pointerInput(groups) {
                detectTapGestures { offset ->
                    val index = HitTest.groupAt(cache.slots, offset.x).takeIf { it >= 0 }
                    selectState.value(if (index == highlighted) null else index)
                }
            }
            .pointerInput(groups) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> selectState.value(HitTest.groupAt(cache.slots, offset.x).takeIf { it >= 0 }) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        selectState.value(HitTest.groupAt(cache.slots, change.position.x).takeIf { it >= 0 })
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
            if (groups == 0) return@Canvas
            val range = BarGeometry.valueRange(data, mode)
            val ticks = NiceScale.ticks(range.min, range.max, max(2, yTickCount), includeZero = true)
            val gap = 6.dp.toPx()
            val tickLayouts = if (showYAxis) {
                ticks.values.map { measurer.measure(valueFormatter.format(it), style = axisStyle, maxLines = 1) }
            } else {
                emptyList()
            }
            val labelHeight = measurer.measure("0", style = axisStyle).size.height.toFloat()
            val area = PlotArea(
                left = if (showYAxis) (tickLayouts.maxOfOrNull { it.size.width } ?: 0) + gap else 0f,
                top = if (showYAxis) labelHeight / 2f else 0f,
                right = size.width,
                bottom = size.height - (if (showXAxis && labels.isNotEmpty()) labelHeight + gap else 0f),
            )
            if (area.width <= 0f || area.height <= 0f) return@Canvas
            val scale = LinearScale.vertical(ticks.min, ticks.max, area)
            val slots = BarGeometry.groupSlots(groups, area)
            cache.slots = slots

            ticks.values.forEachIndexed { index, tick ->
                val y = scale.map(tick)
                if (showGrid || tick == 0.0) {
                    val strong = tick == 0.0 && ticks.min < 0.0
                    drawLine(
                        if (strong) colors.axisLabel.copy(alpha = 0.5f) else colors.grid,
                        Offset(area.left, y),
                        Offset(area.right, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                if (showYAxis) {
                    val layout = tickLayouts[index]
                    drawText(layout, topLeft = Offset(area.left - gap - layout.size.width, y - layout.size.height / 2f))
                }
            }
            if (showXAxis && labels.isNotEmpty()) {
                drawXLabels(measurer, labels, slots.map { it.center }, area.bottom + gap, axisStyle, minGap = 6.dp.toPx())
            }

            if (current != null) {
                val slot = slots[current]
                drawRoundRect(
                    colors.highlight.copy(alpha = 0.10f),
                    topLeft = Offset(slot.start + 2.dp.toPx(), area.top),
                    size = Size(slot.width - 4.dp.toPx(), area.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                )
            }

            val p = progress.value
            val shown = if (p >= 1f) data else data.map { group -> group.map { it * p } }
            val rects = BarGeometry.layout(
                data = shown,
                area = area,
                yMin = ticks.min,
                yMax = ticks.max,
                mode = mode,
                groupSpacing = groupSpacing,
                barSpacing = barSpacing.toPx(),
                stackSpacing = stackSpacing.toPx(),
            )
            val tops = BarGeometry.topSeriesOfStacks(data)
            val bottoms = BarGeometry.bottomSeriesOfStacks(data)
            val radius = cornerRadius.toPx()
            for (rect in rects) {
                val dimmed = current != null && rect.group != current
                val color = barColors[rect.series].let { if (dimmed) it.copy(alpha = it.alpha * 0.45f) else it }
                val stacked = mode == BarMode.Stacked
                val roundTop = rect.value > 0.0 && (!stacked || tops[rect.group] == rect.series)
                val roundBottom = rect.value < 0.0 && (!stacked || bottoms[rect.group] == rect.series)
                drawBar(rect.left, rect.top, rect.right, rect.bottom, color, radius, roundTop, roundBottom)
            }

            if (current != null && p >= 1f) {
                val groupRects = rects.filter { it.group == current }
                val topY = groupRects.minOfOrNull { it.top } ?: area.bottom
                val rows = ArrayList<TooltipRow>()
                series.forEachIndexed { s, item ->
                    val value = valueFormatter.format(data[current][s])
                    rows += if (series.size == 1) TooltipRow(value, null) else TooltipRow("${item.name}  $value", barColors[s])
                }
                if (mode == BarMode.Stacked && series.size > 1) {
                    rows += TooltipRow("Total  ${valueFormatter.format(data[current].sum())}", null)
                }
                drawTooltip(
                    measurer = measurer,
                    title = labels.getOrNull(current),
                    rows = rows,
                    anchorX = slots[current].end - slots[current].width * groupSpacing / 2f,
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
                items = series.mapIndexed { index, item -> LegendItem(item.name, barColors[index]) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * A bar chart of a single series: `BarChart(values = steps, labels = days, title = "Weekly steps")`.
 * See the other overload for every parameter.
 */
@Composable
public fun BarChart(
    values: List<Number>,
    modifier: Modifier = Modifier,
    labels: List<String> = emptyList(),
    title: String = "Bar chart",
    color: Color = MaterialTheme.colorScheme.primary,
    valueFormatter: ValueFormatter = ValueFormatter.Compact,
    colors: ChartColors = ChartDefaults.colors(),
    showGrid: Boolean = true,
    showYAxis: Boolean = true,
    showXAxis: Boolean = true,
    cornerRadius: Dp = 6.dp,
    initialHighlight: Int? = null,
    onHighlightChange: ((Int?) -> Unit)? = null,
    animationSpec: AnimationSpec<Float>? = ChartDefaults.EntryAnimation,
    contentDescription: String? = null,
) {
    BarChart(
        series = listOf(BarSeries(title, values, color)),
        modifier = modifier,
        labels = labels,
        title = title,
        valueFormatter = valueFormatter,
        colors = colors,
        showGrid = showGrid,
        showYAxis = showYAxis,
        showXAxis = showXAxis,
        cornerRadius = cornerRadius,
        showLegend = false,
        initialHighlight = initialHighlight,
        onHighlightChange = onHighlightChange,
        animationSpec = animationSpec,
        contentDescription = contentDescription,
    )
}
