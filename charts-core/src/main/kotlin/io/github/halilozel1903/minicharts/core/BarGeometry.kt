package io.github.halilozel1903.minicharts.core

import kotlin.math.max

/** How several series share a bar slot. */
public enum class BarMode {
    /** Side by side. */
    Grouped,

    /** On top of each other; negative values stack downwards from zero. */
    Stacked,
}

/** A horizontal span in pixels. */
public data class Span(val start: Float, val end: Float) {
    val width: Float get() = end - start
    val center: Float get() = (start + end) / 2f

    public operator fun contains(x: Float): Boolean = x in start..end
}

/**
 * One laid out bar in pixels. [group] is the x slot (a category such as a weekday) and [series] the data set.
 * [top] is always above [bottom]; for a negative value the bar hangs below the zero line.
 */
public data class BarRect(
    val group: Int,
    val series: Int,
    val value: Double,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f

    /** Whether the point lies on the bar, widened by [slop] pixels on each side. */
    public fun contains(x: Float, y: Float, slop: Float = 0f): Boolean =
        x >= left - slop && x <= right + slop && y >= top - slop && y <= bottom + slop
}

/** Bar positions for grouped and stacked bar charts. Data is indexed as `data[group][series]`. */
public object BarGeometry {

    /**
     * The value range the y axis must show, always containing zero. In [BarMode.Stacked] it is the range of
     * the positive and negative stack totals.
     */
    public fun valueRange(data: List<List<Double>>, mode: BarMode): ValueRange {
        var range = ValueRange(0.0, 0.0)
        for (group in data) {
            if (mode == BarMode.Grouped) {
                for (value in group) if (value.isFinite()) range = range.including(value)
            } else {
                range = range.including(group.filter { it.isFinite() && it > 0 }.sum())
                range = range.including(group.filter { it.isFinite() && it < 0 }.sum())
            }
        }
        return range
    }

    /**
     * The horizontal slot of each of [groupCount] groups. Each slot is `area.width / groupCount` wide, and
     * [groupSpacing] (0..1) of it is left empty, half on each side.
     */
    public fun groupSpans(groupCount: Int, area: PlotArea, groupSpacing: Float = 0.3f): List<Span> {
        if (groupCount <= 0) return emptyList()
        val slot = area.width / groupCount
        val barsWidth = slot * (1f - groupSpacing.coerceIn(0f, 0.95f))
        return List(groupCount) { index ->
            val start = area.left + slot * index + (slot - barsWidth) / 2f
            Span(start, start + barsWidth)
        }
    }

    /** The full slot of each group, without spacing; used to find the group under a finger. */
    public fun groupSlots(groupCount: Int, area: PlotArea): List<Span> = groupSpans(groupCount, area, 0f)

    /**
     * Lays out the bars of [data] in [area], with [yMin] at the bottom and [yMax] at the top.
     *
     * - [groupSpacing]: the part of each slot (0..1) left empty between groups.
     * - [barSpacing]: pixels between the bars of one group in [BarMode.Grouped].
     * - [stackSpacing]: pixels between the segments of a stack in [BarMode.Stacked].
     *
     * Every value gets a rect, zero values a flat one, so `rects` can be matched to the data by index.
     * Non-finite values count as zero.
     */
    public fun layout(
        data: List<List<Double>>,
        area: PlotArea,
        yMin: Double,
        yMax: Double,
        mode: BarMode = BarMode.Grouped,
        groupSpacing: Float = 0.3f,
        barSpacing: Float = 4f,
        stackSpacing: Float = 0f,
    ): List<BarRect> {
        val spans = groupSpans(data.size, area, groupSpacing)
        val scale = LinearScale.vertical(yMin, yMax, area)
        val zero = scale.map(0.0.coerceIn(minOf(yMin, yMax), maxOf(yMin, yMax)))
        val rects = ArrayList<BarRect>()
        data.forEachIndexed { groupIndex, group ->
            val span = spans[groupIndex]
            when (mode) {
                BarMode.Grouped -> {
                    val count = group.size
                    if (count == 0) return@forEachIndexed
                    var gap = barSpacing.coerceAtLeast(0f)
                    if (gap * (count - 1) >= span.width) gap = 0f
                    val barWidth = (span.width - gap * (count - 1)) / count
                    group.forEachIndexed { seriesIndex, raw ->
                        val value = if (raw.isFinite()) raw else 0.0
                        val left = span.start + seriesIndex * (barWidth + gap)
                        val y = scale.map(value)
                        rects += BarRect(groupIndex, seriesIndex, value, left, minOf(y, zero), left + barWidth, max(y, zero))
                    }
                }
                BarMode.Stacked -> {
                    var positive = 0.0
                    var negative = 0.0
                    var positiveSegments = 0
                    var negativeSegments = 0
                    group.forEachIndexed { seriesIndex, raw ->
                        val value = if (raw.isFinite()) raw else 0.0
                        val rect = if (value >= 0.0) {
                            val from = scale.map(positive)
                            positive += value
                            val to = scale.map(positive)
                            // Leave a gap above the previous segment of the stack.
                            val inset = if (positiveSegments > 0 && value > 0.0) stackSpacing else 0f
                            if (value > 0.0) positiveSegments++
                            BarRect(groupIndex, seriesIndex, value, span.start, to, span.end, max(to, from - inset))
                        } else {
                            val from = scale.map(negative)
                            negative += value
                            val to = scale.map(negative)
                            val inset = if (negativeSegments > 0) stackSpacing else 0f
                            negativeSegments++
                            BarRect(groupIndex, seriesIndex, value, span.start, minOf(to, from + inset), span.end, to)
                        }
                        rects += rect
                    }
                }
            }
        }
        return rects
    }

    /**
     * For each group, the series index of the outermost non-zero positive segment of a stack (the one that
     * gets rounded corners), or -1 when the group has no positive value.
     */
    public fun topSeriesOfStacks(data: List<List<Double>>): List<Int> =
        data.map { group -> group.indexOfLast { it.isFinite() && it > 0.0 } }

    /** Like [topSeriesOfStacks] for the negative part of each stack. */
    public fun bottomSeriesOfStacks(data: List<List<Double>>): List<Int> =
        data.map { group -> group.indexOfLast { it.isFinite() && it < 0.0 } }
}
