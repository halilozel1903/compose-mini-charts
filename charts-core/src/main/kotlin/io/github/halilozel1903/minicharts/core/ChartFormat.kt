package io.github.halilozel1903.minicharts.core

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.floor

/** Turns a value into the text shown on axes, tooltips and in accessibility summaries. */
public fun interface ValueFormatter {
    public fun format(value: Double): String

    public companion object {
        /** 950, 3.2k, 9.8k, 1.2M. */
        public val Compact: ValueFormatter = ValueFormatter { ChartFormat.compact(it) }

        /** 12,345.68 with up to two decimals. */
        public val Plain: ValueFormatter = ValueFormatter { ChartFormat.number(it) }

        /** Integers with thousands separators: 12,346. */
        public val Integer: ValueFormatter = ValueFormatter { ChartFormat.number(it, maxDecimals = 0) }

        /** [prefix] before and [suffix] after another formatter's text, for example "$3.2k" or "42 kg". */
        public fun affixed(prefix: String = "", suffix: String = "", base: ValueFormatter = Compact): ValueFormatter =
            ValueFormatter { value ->
                val text = base.format(abs(value))
                if (value < 0 && text.any { it in '1'..'9' }) "-$prefix$text$suffix" else "$prefix$text$suffix"
            }
    }
}

/**
 * Locale independent number formatting, so output is the same on every device and in tests.
 * Rounding is half up (2.5 becomes 3) and trailing zeros are dropped.
 */
public object ChartFormat {
    private val units = listOf("", "k", "M", "B", "T")

    /** [value] with at most [maxDecimals] decimals and, with [grouping], a comma every three digits. */
    public fun number(value: Double, maxDecimals: Int = 2, grouping: Boolean = true): String {
        if (value.isNaN()) return "NaN"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"
        val rounded = round(value, maxDecimals)
        val plain = rounded.abs().stripTrailingZeros().toPlainString()
        val negative = rounded.signum() < 0
        val integerPart = plain.substringBefore('.')
        val fractionPart = plain.substringAfter('.', missingDelimiterValue = "")
        val grouped = if (grouping) group(integerPart) else integerPart
        val body = if (fractionPart.isEmpty()) grouped else "$grouped.$fractionPart"
        return if (negative) "-$body" else body
    }

    /** Short numbers for small spaces: 950, 3.2k, 9.8k, 12k, 1.2M, 3B. 999,950 becomes 1M, not 1000k. */
    public fun compact(value: Double, maxDecimals: Int = 1): String {
        if (!value.isFinite()) return number(value)
        var unit = 0
        var scaled = value
        while (unit < units.lastIndex && abs(round(scaled, maxDecimals).toDouble()) >= 1000.0) {
            unit++
            scaled = value / Math.pow(1000.0, unit.toDouble())
        }
        return number(scaled, maxDecimals, grouping = true) + units[unit]
    }

    /** A fraction (0.425) as a percentage ("43%" with no decimals, "42.5%" with one). */
    public fun percent(fraction: Double, maxDecimals: Int = 0): String = number(fraction * 100.0, maxDecimals) + "%"

    /**
     * The share of each value in percent, rounded to [decimals] with the largest remainder method, so the
     * results always add up to exactly 100 (when the total is positive). Negative and non-finite values
     * count as zero.
     */
    public fun percentages(values: List<Double>, decimals: Int = 0): List<Double> {
        val positive = values.map { if (it.isFinite() && it > 0.0) it else 0.0 }
        val total = positive.sum()
        if (total <= 0.0) return values.map { 0.0 }
        val scale = Math.pow(10.0, decimals.toDouble())
        val exact = positive.map { it / total * 100.0 * scale }
        val floors = exact.map { floor(it + 1e-9) }
        var missing = Math.round(100.0 * scale - floors.sum()).toInt()
        val result = floors.toMutableList()
        val order = exact.indices.sortedByDescending { exact[it] - floors[it] }
        for (index in order) {
            if (missing <= 0) break
            if (positive[index] == 0.0) continue
            result[index] += 1.0
            missing--
        }
        return result.map { it / scale }
    }

    private fun round(value: Double, decimals: Int): BigDecimal =
        BigDecimal.valueOf(value).setScale(decimals.coerceAtLeast(0), RoundingMode.HALF_UP)

    private fun group(digits: String): String {
        if (digits.length <= 3) return digits
        val builder = StringBuilder()
        val head = digits.length % 3
        if (head > 0) builder.append(digits, 0, head)
        var index = head
        while (index < digits.length) {
            if (builder.isNotEmpty()) builder.append(',')
            builder.append(digits, index, index + 3)
            index += 3
        }
        return builder.toString()
    }
}
