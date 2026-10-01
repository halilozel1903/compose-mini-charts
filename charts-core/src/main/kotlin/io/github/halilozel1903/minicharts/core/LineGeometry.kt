package io.github.halilozel1903.minicharts.core

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

/** A point in pixels. */
public data class ChartPoint(val x: Float, val y: Float)

/** How a line connects its points. */
public enum class LineCurve {
    /** Straight segments. */
    Straight,

    /** Monotone cubic curves: smooth, but never above or below the data between two points. */
    Smooth,
}

/** One drawing command of a line path. */
public sealed interface PathSegment {
    /** The point the segment ends at. */
    public val end: ChartPoint

    public data class MoveTo(override val end: ChartPoint) : PathSegment

    public data class LineTo(override val end: ChartPoint) : PathSegment

    public data class CubicTo(val control1: ChartPoint, val control2: ChartPoint, override val end: ChartPoint) : PathSegment
}

/** Point positions and path commands for line charts and sparklines. */
public object LineGeometry {

    /**
     * The x positions of [count] evenly spaced points from the left to the right edge of [area].
     * A single point sits in the middle.
     */
    public fun xPositions(count: Int, area: PlotArea): List<Float> = when {
        count <= 0 -> emptyList()
        count == 1 -> listOf(area.left + area.width / 2f)
        else -> List(count) { index -> area.left + area.width * index / (count - 1) }
    }

    /**
     * The pixel positions of [values] in [area], with [yMin] at the bottom and [yMax] at the top.
     * [slots] is the number of x positions; pass the longest series' size so shorter series line up.
     */
    public fun points(
        values: List<Double>,
        area: PlotArea,
        yMin: Double,
        yMax: Double,
        slots: Int = values.size,
    ): List<ChartPoint> {
        val xs = xPositions(maxOf(slots, values.size), area)
        val scale = LinearScale.vertical(yMin, yMax, area)
        return values.mapIndexed { index, value -> ChartPoint(xs[index], scale.map(value)) }
    }

    /** The path through [points]: a move to the first point, then one line or curve per following point. */
    public fun segments(points: List<ChartPoint>, curve: LineCurve = LineCurve.Smooth): List<PathSegment> {
        if (points.isEmpty()) return emptyList()
        val result = ArrayList<PathSegment>(points.size)
        result += PathSegment.MoveTo(points[0])
        if (curve == LineCurve.Straight || points.size < 3) {
            for (i in 1 until points.size) result += PathSegment.LineTo(points[i])
            return result
        }
        val tangents = monotoneTangents(points)
        for (i in 0 until points.size - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val third = (p1.x - p0.x) / 3f
            result += PathSegment.CubicTo(
                control1 = ChartPoint(p0.x + third, (p0.y + tangents[i] * third).toFloat()),
                control2 = ChartPoint(p1.x - third, (p1.y - tangents[i + 1] * third).toFloat()),
                end = p1,
            )
        }
        return result
    }

    /**
     * The slope at each point for monotone cubic interpolation (Steffen's method, as used by d3's
     * curveMonotoneX). The curve between two points never leaves the band between their y values, so a
     * smoothed line has no false peaks or dips. Points must be sorted by x.
     */
    public fun monotoneTangents(points: List<ChartPoint>): List<Double> {
        val n = points.size
        if (n < 2) return List(n) { 0.0 }
        val widths = DoubleArray(n - 1) { (points[it + 1].x - points[it].x).toDouble() }
        val slopes = DoubleArray(n - 1) {
            if (widths[it] == 0.0) 0.0 else (points[it + 1].y - points[it].y) / widths[it]
        }
        val tangents = DoubleArray(n)
        tangents[0] = slopes[0]
        tangents[n - 1] = slopes[n - 2]
        for (i in 1 until n - 1) {
            val s0 = slopes[i - 1]
            val s1 = slopes[i]
            val h0 = widths[i - 1]
            val h1 = widths[i]
            val p = if (h0 + h1 == 0.0) 0.0 else (s0 * h1 + s1 * h0) / (h0 + h1)
            tangents[i] = (sign(s0) + sign(s1)) * min(min(abs(s0), abs(s1)), 0.5 * abs(p))
        }
        return tangents.toList()
    }
}
