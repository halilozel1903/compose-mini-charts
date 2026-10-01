package io.github.halilozel1903.minicharts

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import io.github.halilozel1903.minicharts.core.ChartFormat
import io.github.halilozel1903.minicharts.core.ChartSummary
import io.github.halilozel1903.minicharts.core.DonutGeometry
import io.github.halilozel1903.minicharts.core.ValueFormatter
import kotlin.math.max
import kotlin.math.min

/** One slice of a [DonutChart]. An unspecified [color] takes the next series color of [ChartColors]. */
@Immutable
public data class DonutSlice(
    val label: String,
    val value: Number,
    val color: Color = Color.Unspecified,
)

/** Where a [DonutChart] puts its legend. */
public enum class LegendPosition {
    /** Under the ring, centred and wrapping. */
    Bottom,

    /** Beside the ring, one slice per line with its share. */
    End,

    /** No legend. */
    None,
}

/**
 * A donut chart with a label in the middle and a legend. Tap a slice (or a legend item) to highlight it: it
 * grows, the others fade, and the middle shows its label and share. Tap it again to go back to the total.
 *
 * @param title What the chart shows; starts the accessibility summary ("Spending, 4 slices, total 2.4k: ...").
 * @param centerLabel The caption in the middle; null hides it.
 * @param centerValue The big text in the middle; null shows the total formatted with [valueFormatter].
 * @param thickness Ring thickness; null uses 18% of the diameter.
 * @param gapDegrees Space between slices.
 * @param startAngle Where the first slice starts; -90 is twelve o'clock.
 * @param showLegendValues Whether legend items show each slice's share.
 * @param animationSpec The entry animation (the ring unfolds); null shows the final chart at once.
 */
@Composable
public fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    title: String = "Donut chart",
    centerLabel: String? = title,
    centerValue: String? = null,
    valueFormatter: ValueFormatter = ValueFormatter.Compact,
    colors: ChartColors = ChartDefaults.colors(),
    thickness: Dp? = null,
    gapDegrees: Float = 2f,
    startAngle: Float = -90f,
    legendPosition: LegendPosition = LegendPosition.Bottom,
    showLegendValues: Boolean = true,
    percentDecimals: Int = 0,
    highlightEnabled: Boolean = true,
    initialHighlight: Int? = null,
    onHighlightChange: ((Int?) -> Unit)? = null,
    animationSpec: AnimationSpec<Float>? = ChartDefaults.EntryAnimation,
    contentDescription: String? = null,
) {
    val values = remember(slices) {
        slices.map { slice -> slice.value.toDouble().let { if (it.isFinite() && it > 0.0) it else 0.0 } }
    }
    val total = values.sum()
    val shares = remember(values, percentDecimals) { ChartFormat.percentages(values, percentDecimals) }
    val sliceColors = slices.mapIndexed { index, slice -> colors.resolve(slice.color, index) }

    var highlighted by rememberSaveable { mutableStateOf<Int?>(initialHighlight) }
    val current = highlighted?.takeIf { it in slices.indices && values[it] > 0.0 }
    val onChange by rememberUpdatedState(onHighlightChange)
    val toggle: (Int?) -> Unit = { index ->
        val next = if (index == highlighted) null else index
        if (next != highlighted) {
            highlighted = next
            onChange?.invoke(next)
        }
    }

    val summary = contentDescription ?: remember(values, slices, title, valueFormatter, percentDecimals) {
        ChartSummary.donut(title, slices.map { it.label }, values, valueFormatter, percentDecimals)
    }
    val percentText: (Int) -> String = { index -> ChartFormat.number(shares[index], percentDecimals) + "%" }
    val stateText = current?.let { "${slices[it].label}: ${valueFormatter.format(values[it])}, ${percentText(it)}" }

    val legendItems = slices.mapIndexed { index, slice ->
        LegendItem(slice.label, sliceColors[index], if (showLegendValues) percentText(index) else null)
    }
    val onLegendClick: ((Int) -> Unit)? = if (highlightEnabled) toggle else null

    val ring: @Composable (Modifier) -> Unit = { ringModifier ->
        DonutRing(
            values = values,
            colors = sliceColors,
            chartColors = colors,
            current = current,
            centerTitle = if (current != null) slices[current].label else centerLabel,
            centerText = if (current != null) percentText(current) else centerValue ?: valueFormatter.format(total),
            thickness = thickness,
            gapDegrees = gapDegrees,
            startAngle = startAngle,
            highlightEnabled = highlightEnabled,
            onTap = toggle,
            animationSpec = animationSpec,
            modifier = ringModifier.semantics {
                this.contentDescription = summary
                if (stateText != null) stateDescription = stateText
            },
        )
    }

    BoxWithConstraints(modifier) {
        val bounded = constraints.hasBoundedHeight
        val fallback = min(maxWidth, 220.dp)
        val besideLegend = min(maxWidth / 2, 220.dp)
        when (legendPosition) {
            LegendPosition.End -> Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ring(
                    if (bounded) Modifier.weight(1f).fillMaxHeight() else Modifier.weight(1f).height(besideLegend),
                )
                ChartLegend(
                    items = legendItems,
                    modifier = Modifier.weight(1f),
                    vertical = true,
                    selectedIndex = current,
                    onItemClick = onLegendClick,
                )
            }
            LegendPosition.Bottom -> Column(
                if (bounded) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ring(if (bounded) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth().height(fallback))
                Spacer(Modifier.height(12.dp))
                ChartLegend(
                    items = legendItems,
                    modifier = Modifier.fillMaxWidth(),
                    selectedIndex = current,
                    onItemClick = onLegendClick,
                )
            }
            LegendPosition.None -> ring(if (bounded) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(fallback))
        }
    }
}

@Composable
private fun DonutRing(
    values: List<Double>,
    colors: List<Color>,
    chartColors: ChartColors,
    current: Int?,
    centerTitle: String?,
    centerText: String?,
    thickness: Dp?,
    gapDegrees: Float,
    startAngle: Float,
    highlightEnabled: Boolean,
    onTap: (Int?) -> Unit,
    animationSpec: AnimationSpec<Float>?,
    modifier: Modifier,
) {
    val measurer = rememberTextMeasurer()
    val progress = rememberEntryProgress(values, animationSpec)
    val cache = remember { HitCache() }
    val tap by rememberUpdatedState(onTap)
    val typography = MaterialTheme.typography
    val bigValue: TextStyle = typography.headlineSmall.copy(color = chartColors.centerValue, fontWeight = FontWeight.SemiBold)
    val smallValue: TextStyle = typography.titleSmall.copy(color = chartColors.centerValue, fontWeight = FontWeight.SemiBold)
    val caption: TextStyle = typography.labelMedium.copy(color = chartColors.centerLabel)

    val touch = if (highlightEnabled) {
        Modifier.pointerInput(values) {
            detectTapGestures { offset ->
                val dx = offset.x - cache.center.x
                val dy = offset.y - cache.center.y
                val index = DonutGeometry.hitTest(cache.arcs, dx, dy, cache.innerRadius * 0.8f, cache.outerRadius * 1.1f)
                if (index != null) tap(index)
            }
        }
    } else {
        Modifier
    }

    Canvas(modifier.then(touch)) {
        val grow = 6.dp.toPx()
        val diameter = min(size.width, size.height) - grow * 2
        if (diameter <= 0f) return@Canvas
        val stroke = thickness?.toPx()?.coerceAtMost(diameter / 2f) ?: (diameter * 0.18f)
        val radius = (diameter - stroke) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val arcs = DonutGeometry.arcs(values, startAngle, gapDegrees)
        cache.arcs = arcs
        cache.center = center
        cache.innerRadius = radius - stroke / 2f
        cache.outerRadius = radius + stroke / 2f + grow

        val p = progress.value
        if (values.sum() <= 0.0) {
            drawCircle(chartColors.track, radius = radius, center = center, style = Stroke(stroke))
        }
        for (arc in arcs) {
            if (arc.sweepAngle <= 0f) continue
            val selected = arc.index == current
            val dimmed = current != null && !selected
            val width = if (selected) stroke + grow else stroke
            val r = if (selected) radius + grow / 2f else radius
            val color = colors[arc.index].let { if (dimmed) it.copy(alpha = it.alpha * 0.4f) else it }
            drawArc(
                color = color,
                startAngle = startAngle + (arc.startAngle - startAngle) * p,
                sweepAngle = arc.sweepAngle * p,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = width, cap = StrokeCap.Butt),
            )
        }

        val hole = (radius - stroke / 2f) * 2f * 0.84f
        if (hole <= 0f) return@Canvas
        val maxWidth = Constraints(maxWidth = max(1, hole.toInt()))
        val valueStyle = if (hole > 110.dp.toPx()) bigValue else smallValue
        val valueLayout = centerText?.let {
            measurer.measure(it, style = valueStyle, maxLines = 1, overflow = TextOverflow.Ellipsis, constraints = maxWidth)
        }
        val captionLayout = centerTitle?.let {
            measurer.measure(it, style = caption, maxLines = 1, overflow = TextOverflow.Ellipsis, constraints = maxWidth)
        }
        val height = (valueLayout?.size?.height ?: 0) + (captionLayout?.size?.height ?: 0)
        var y = center.y - height / 2f
        if (valueLayout != null) {
            drawText(valueLayout, topLeft = Offset(center.x - valueLayout.size.width / 2f, y))
            y += valueLayout.size.height
        }
        if (captionLayout != null) {
            drawText(captionLayout, topLeft = Offset(center.x - captionLayout.size.width / 2f, y))
        }
    }
}
