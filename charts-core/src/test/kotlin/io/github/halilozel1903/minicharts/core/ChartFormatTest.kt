package io.github.halilozel1903.minicharts.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ChartFormatTest {

    @Test
    fun numberRoundsHalfUpAndGroups() {
        assertEquals("12,345.68", ChartFormat.number(12_345.678))
        assertEquals("2.68", ChartFormat.number(2.675))
        assertEquals("3", ChartFormat.number(2.5, maxDecimals = 0))
        assertEquals("1,000,000", ChartFormat.number(1e6))
        assertEquals("-1,234.5", ChartFormat.number(-1_234.5))
        assertEquals("0", ChartFormat.number(-0.001))
        assertEquals("12345", ChartFormat.number(12_345.0, grouping = false))
    }

    @Test
    fun compactUnits() {
        assertEquals("950", ChartFormat.compact(950.0))
        assertEquals("3.2k", ChartFormat.compact(3_210.0))
        assertEquals("9.8k", ChartFormat.compact(9_840.0))
        assertEquals("12k", ChartFormat.compact(12_000.0))
        assertEquals("1.2M", ChartFormat.compact(1_234_567.0))
        assertEquals("3B", ChartFormat.compact(3e9))
        assertEquals("-4.5k", ChartFormat.compact(-4_500.0))
        assertEquals("0.25", ChartFormat.compact(0.25, maxDecimals = 2))
    }

    @Test
    fun compactPromotesWhenRoundingReachesTheNextUnit() {
        assertEquals("1M", ChartFormat.compact(999_950.0))
        assertEquals("1k", ChartFormat.compact(999.96))
    }

    @Test
    fun percent() {
        assertEquals("43%", ChartFormat.percent(0.425))
        assertEquals("42.5%", ChartFormat.percent(0.425, maxDecimals = 1))
        assertEquals("100%", ChartFormat.percent(1.0))
    }

    @Test
    fun percentagesAlwaysAddUpTo100() {
        val shares = ChartFormat.percentages(listOf(1.0, 1.0, 1.0))
        assertEquals(listOf(34.0, 33.0, 33.0), shares)
        assertEquals(100.0, ChartFormat.percentages(listOf(13.0, 27.0, 41.0, 19.5, 0.7)).sum(), 1e-9)
        val decimals = ChartFormat.percentages(listOf(1.0, 2.0, 4.0), decimals = 1)
        assertEquals(100.0, decimals.sum(), 1e-9)
        assertEquals(listOf(14.3, 28.6, 57.1), decimals)
        assertEquals(listOf(0.0, 0.0), ChartFormat.percentages(listOf(0.0, -1.0)))
        assertEquals(listOf(100.0, 0.0), ChartFormat.percentages(listOf(5.0, 0.0)))
    }

    @Test
    fun formatters() {
        assertEquals("$3.2k", ValueFormatter.affixed(prefix = "$").format(3_200.0))
        assertEquals("-$3.2k", ValueFormatter.affixed(prefix = "$").format(-3_200.0))
        assertEquals("42 kg", ValueFormatter.affixed(suffix = " kg", base = ValueFormatter.Integer).format(41.6))
        assertEquals("1,234.5", ValueFormatter.Plain.format(1_234.5))
    }
}
