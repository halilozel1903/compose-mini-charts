package io.github.halilozel1903.minicharts.core

import kotlin.math.abs
import kotlin.math.hypot

/** Finding the data under a finger. */
public object HitTest {

    /**
     * The index of the x position in [xs] (sorted ascending) closest to [x], or -1 when [xs] is empty.
     * On a tie the left one wins. This is what a touch highlight on a line chart uses: the whole height counts.
     */
    public fun nearestIndex(xs: List<Float>, x: Float): Int {
        if (xs.isEmpty()) return -1
        var low = 0
        var high = xs.size - 1
        while (low < high) {
            val mid = (low + high) ushr 1
            if (xs[mid] < x) low = mid + 1 else high = mid
        }
        // low is the first position >= x; compare it with its left neighbour.
        if (low > 0 && abs(xs[low - 1] - x) <= abs(xs[low] - x)) return low - 1
        return low
    }

    /**
     * The index of the point closest to ([x], [y]) in a straight line, or null when none is within
     * [maxDistance] pixels.
     */
    public fun nearestPoint(points: List<ChartPoint>, x: Float, y: Float, maxDistance: Float = Float.POSITIVE_INFINITY): Int? {
        var best: Int? = null
        var bestDistance = maxDistance
        points.forEachIndexed { index, point ->
            val distance = hypot(point.x - x, point.y - y)
            if (distance <= bestDistance) {
                if (best == null || distance < bestDistance) {
                    best = index
                    bestDistance = distance
                }
            }
        }
        return best
    }

    /** The bar under ([x], [y]), widened by [slop] pixels so thin bars are easy to hit, or null. */
    public fun barAt(rects: List<BarRect>, x: Float, y: Float, slop: Float = 0f): BarRect? =
        rects.lastOrNull { it.contains(x, y, slop) }

    /** The index of the group slot containing [x], or the nearest one; -1 when there are no slots. */
    public fun groupAt(slots: List<Span>, x: Float): Int {
        if (slots.isEmpty()) return -1
        val inside = slots.indexOfFirst { x in it }
        if (inside >= 0) return inside
        return slots.indices.minBy { abs(slots[it].center - x) }
    }
}
