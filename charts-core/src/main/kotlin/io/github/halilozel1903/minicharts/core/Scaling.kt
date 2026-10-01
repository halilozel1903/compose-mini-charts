package io.github.halilozel1903.minicharts.core

/** The smallest and largest of some values. */
public data class ValueRange(val min: Double, val max: Double) {
    /** max - min. */
    val span: Double get() = max - min

    /** This range widened to contain [value]. */
    public fun including(value: Double): ValueRange = ValueRange(minOf(min, value), maxOf(max, value))

    public companion object {
        /** The range of the finite [values], or null when there are none. */
        public fun of(values: Iterable<Double>): ValueRange? {
            var low = Double.POSITIVE_INFINITY
            var high = Double.NEGATIVE_INFINITY
            for (value in values) {
                if (!value.isFinite()) continue
                if (value < low) low = value
                if (value > high) high = value
            }
            return if (low <= high) ValueRange(low, high) else null
        }
    }
}

/** The rectangle the data is drawn in, in pixels. y grows downwards, as on a canvas. */
public data class PlotArea(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    /** Whether the point is inside the area, edges included. */
    public fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

/** Maps values linearly from a domain to a pixel range and back. The pixel range may run backwards. */
public class LinearScale(
    public val domainStart: Double,
    public val domainEnd: Double,
    public val rangeStart: Float,
    public val rangeEnd: Float,
) {
    /** The pixel position of [value]. A domain of zero width maps everything to the middle of the range. */
    public fun map(value: Double): Float {
        val span = domainEnd - domainStart
        if (span == 0.0) return (rangeStart + rangeEnd) / 2f
        val t = (value - domainStart) / span
        return (rangeStart + t * (rangeEnd - rangeStart)).toFloat()
    }

    /** The value at pixel [position]. */
    public fun invert(position: Float): Double {
        val pixels = rangeEnd - rangeStart
        if (pixels == 0f) return (domainStart + domainEnd) / 2.0
        val t = (position - rangeStart).toDouble() / pixels
        return domainStart + t * (domainEnd - domainStart)
    }

    public companion object {
        /** The usual y scale: [yMin] at the bottom of [area] and [yMax] at its top. */
        public fun vertical(yMin: Double, yMax: Double, area: PlotArea): LinearScale =
            LinearScale(yMin, yMax, area.bottom, area.top)
    }
}

/** Data helpers. */
public object ChartData {
    /**
     * Scales [values] to 0..1, the smallest value becoming 0 and the largest 1. When all values are equal they
     * all become 0.5. Non-finite values stay as they are.
     */
    public fun normalize(values: List<Double>): List<Double> {
        val range = ValueRange.of(values) ?: return values
        if (range.span == 0.0) return values.map { if (it.isFinite()) 0.5 else it }
        return values.map { if (it.isFinite()) (it - range.min) / range.span else it }
    }

    /** Values as doubles, with non-finite ones replaced by 0. Handy for `List<Int>` or `List<Float>` input. */
    public fun sanitize(values: List<Number>): List<Double> =
        values.map { it.toDouble().let { value -> if (value.isFinite()) value else 0.0 } }
}
