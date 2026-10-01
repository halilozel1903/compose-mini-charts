package io.github.halilozel1903.minicharts.core

import kotlin.math.atan2
import kotlin.math.hypot

/**
 * One slice of a donut or pie in degrees, in canvas convention: 0 is three o'clock and angles grow clockwise.
 * [fraction] is the share of the total (0..1) and does not depend on the gaps.
 */
public data class DonutArc(
    val index: Int,
    val value: Double,
    val fraction: Double,
    val startAngle: Float,
    val sweepAngle: Float,
) {
    val endAngle: Float get() = startAngle + sweepAngle
    val midAngle: Float get() = startAngle + sweepAngle / 2f

    /** Whether [angle] (any number of turns) falls on this arc. */
    public fun containsAngle(angle: Float): Boolean {
        if (sweepAngle <= 0f) return false
        return normalizeAngle(angle - startAngle) < sweepAngle
    }
}

/** Arc angles and hit testing for donut charts. */
public object DonutGeometry {

    /**
     * One arc per value, starting at [startAngle] (-90 is twelve o'clock) and going clockwise.
     * Zero, negative and non-finite values get an empty arc. [gapDegrees] is left between neighbouring slices
     * (only when there are at least two), centred on the slice borders, and is capped so slices never vanish.
     */
    public fun arcs(values: List<Double>, startAngle: Float = -90f, gapDegrees: Float = 0f): List<DonutArc> {
        val positive = values.map { if (it.isFinite() && it > 0.0) it else 0.0 }
        val total = positive.sum()
        val count = positive.count { it > 0.0 }
        val gap = if (count > 1) gapDegrees.coerceIn(0f, 180f / count) else 0f
        val available = 360f - gap * count
        var cursor = startAngle + gap / 2f
        return positive.mapIndexed { index, value ->
            val fraction = if (total > 0.0) value / total else 0.0
            val sweep = (fraction * available).toFloat()
            val arc = DonutArc(index, values[index], fraction, cursor, sweep)
            if (value > 0.0) cursor += sweep + gap
            arc
        }
    }

    /** The angle of the vector ([dx], [dy]) from the centre, in degrees 0..360, canvas convention. */
    public fun angleOf(dx: Float, dy: Float): Float =
        normalizeAngle(Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat())

    /**
     * The index of the slice at the offset ([dx], [dy]) from the centre, or null when the point is in the hole,
     * outside the ring or in a gap. Pass 0 as [innerRadius] for a pie.
     */
    public fun hitTest(arcs: List<DonutArc>, dx: Float, dy: Float, innerRadius: Float, outerRadius: Float): Int? {
        val distance = hypot(dx, dy)
        if (distance < innerRadius || distance > outerRadius) return null
        val angle = angleOf(dx, dy)
        return arcs.firstOrNull { it.containsAngle(angle) }?.index
    }
}

/** [angle] in 0 until 360. */
internal fun normalizeAngle(angle: Float): Float {
    val result = angle % 360f
    return if (result < 0f) result + 360f else result
}
