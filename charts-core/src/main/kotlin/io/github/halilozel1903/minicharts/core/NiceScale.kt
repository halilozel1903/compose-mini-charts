package io.github.halilozel1903.minicharts.core

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Axis ticks for a value range: the axis runs from [min] to [max] in steps of [step], and [values] holds
 * every tick from [min] to [max] inclusive. [min] and [max] always cover the data the ticks were made for.
 */
public data class AxisTicks(
    val min: Double,
    val max: Double,
    val step: Double,
    val values: List<Double>,
) {
    /** Digits after the decimal point that the tick values need, for example 1 for a step of 0.5. */
    val fractionDigits: Int get() = fractionDigitsFor(step)
}

/**
 * "Nice" axis ticks after Paul Heckbert's algorithm (Graphics Gems, 1990): steps are 1, 2 or 5 times a
 * power of ten, and the axis is widened to whole steps so it starts and ends on a tick.
 */
public object NiceScale {

    /**
     * The nice number close to [value]: 1, 2, 5 or 10 times a power of ten.
     * With [round] the closest one is used, otherwise the smallest one that is not below [value].
     */
    public fun niceNumber(value: Double, round: Boolean): Double {
        require(value > 0.0 && value.isFinite()) { "value must be positive and finite, was $value" }
        val exponent = floor(log10(value))
        val power = 10.0.pow(exponent)
        val fraction = value / power
        val nice = if (round) {
            when {
                fraction < 1.5 -> 1.0
                fraction < 3.0 -> 2.0
                fraction < 7.0 -> 5.0
                else -> 10.0
            }
        } else {
            when {
                fraction <= 1.0 -> 1.0
                fraction <= 2.0 -> 2.0
                fraction <= 5.0 -> 5.0
                else -> 10.0
            }
        }
        return nice * power
    }

    /**
     * Ticks for data between [min] and [max], aiming at about [targetCount] ticks (the result can have one or
     * two more or fewer). The arguments may be in any order. With [includeZero] the axis always contains 0,
     * which bar charts need. A flat range (min == max) is widened so the line sits in the middle.
     */
    public fun ticks(min: Double, max: Double, targetCount: Int = 5, includeZero: Boolean = false): AxisTicks {
        require(min.isFinite() && max.isFinite()) { "min and max must be finite, were $min and $max" }
        require(targetCount >= 2) { "targetCount must be at least 2, was $targetCount" }
        var low = minOf(min, max)
        var high = maxOf(min, max)
        if (includeZero) {
            low = minOf(low, 0.0)
            high = maxOf(high, 0.0)
        }
        if (low == high) {
            if (low == 0.0) {
                high = 1.0
            } else {
                // Widen a flat range by half its value on each side, without crossing zero.
                val pad = abs(low) * 0.5
                low = if (low > 0.0) maxOf(0.0, low - pad) else low - pad
                high = if (high < 0.0) minOf(0.0, high + pad) else high + pad
            }
        }
        val range = niceNumber(high - low, round = false)
        val step = niceNumber(range / (targetCount - 1), round = true)
        val first = floor(low / step + 1e-9).roundToLong()
        val last = ceil(high / step - 1e-9).roundToLong()
        val digits = fractionDigitsFor(step)
        val values = (first..last).map { clean(it * step, digits) }
        return AxisTicks(min = values.first(), max = values.last(), step = step, values = values)
    }
}

internal fun fractionDigitsFor(step: Double): Int =
    if (step <= 0.0 || !step.isFinite()) 0 else max(0, -floor(log10(step) + 1e-9).toInt())

/** Removes floating point noise such as 0.30000000000000004, and turns -0.0 into 0.0. */
internal fun clean(value: Double, digits: Int): Double {
    val factor = 10.0.pow(digits)
    return (value * factor).roundToLong() / factor + 0.0
}
