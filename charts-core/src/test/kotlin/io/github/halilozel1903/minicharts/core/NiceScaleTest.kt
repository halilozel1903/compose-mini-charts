package io.github.halilozel1903.minicharts.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NiceScaleTest {

    @Test
    fun niceNumberRoundsToOneTwoFive() {
        assertEquals(1.0, NiceScale.niceNumber(1.2, round = true))
        assertEquals(2.0, NiceScale.niceNumber(2.4, round = true))
        assertEquals(5.0, NiceScale.niceNumber(4.0, round = true))
        assertEquals(10.0, NiceScale.niceNumber(8.0, round = true))
        assertEquals(500.0, NiceScale.niceNumber(420.0, round = true))
        assertEquals(0.2, NiceScale.niceNumber(0.17, round = true), 1e-12)
    }

    @Test
    fun niceNumberCeilingNeverGoesBelow() {
        assertEquals(1.0, NiceScale.niceNumber(1.0, round = false))
        assertEquals(2.0, NiceScale.niceNumber(1.1, round = false))
        assertEquals(5.0, NiceScale.niceNumber(2.1, round = false))
        assertEquals(10.0, NiceScale.niceNumber(5.1, round = false))
    }

    @Test
    fun classicHeckbertExample() {
        val ticks = NiceScale.ticks(3_200.0, 9_800.0, targetCount = 5)
        assertEquals(listOf(2_000.0, 4_000.0, 6_000.0, 8_000.0, 10_000.0), ticks.values)
        assertEquals(2_000.0, ticks.min)
        assertEquals(10_000.0, ticks.max)
        assertEquals(2_000.0, ticks.step)
    }

    @Test
    fun ticksAlwaysCoverTheData() {
        val ranges = listOf(0.0 to 1.0, -3.7 to 12.2, 0.001 to 0.0042, 97.0 to 103.0, -250.0 to -20.0, 1e6 to 4.2e6)
        for ((low, high) in ranges) {
            val ticks = NiceScale.ticks(low, high)
            assertTrue(ticks.min <= low && ticks.max >= high, "$low..$high -> $ticks")
            assertTrue(ticks.values.size in 3..9, "$low..$high -> ${ticks.values.size} ticks")
            ticks.values.zipWithNext { a, b -> assertEquals(ticks.step, b - a, ticks.step * 1e-9) }
        }
    }

    @Test
    fun tickValuesHaveNoFloatingPointNoise() {
        // 3 * 0.2 is 0.6000000000000001 in floating point.
        val ticks = NiceScale.ticks(0.1, 0.7, targetCount = 7)
        assertEquals(listOf(0.0, 0.2, 0.4, 0.6, 0.8), ticks.values)
        assertEquals(1, ticks.fractionDigits)
    }

    @Test
    fun includeZeroForBars() {
        val ticks = NiceScale.ticks(40.0, 95.0, includeZero = true)
        assertEquals(0.0, ticks.min)
        assertEquals(100.0, ticks.max)
    }

    @Test
    fun argumentsInAnyOrder() {
        assertEquals(NiceScale.ticks(1.0, 9.0), NiceScale.ticks(9.0, 1.0))
    }

    @Test
    fun flatDataIsWidenedAroundTheValue() {
        val ticks = NiceScale.ticks(5.0, 5.0)
        assertTrue(ticks.min < 5.0 && ticks.max > 5.0, ticks.toString())
        assertTrue(ticks.min >= 0.0, "a positive flat line should not get a negative axis: $ticks")
        val zero = NiceScale.ticks(0.0, 0.0)
        assertEquals(0.0, zero.min)
        assertEquals(1.0, zero.max)
    }

    @Test
    fun negativeZeroIsCleaned() {
        val ticks = NiceScale.ticks(-10.0, 10.0)
        assertTrue(ticks.values.contains(0.0))
        assertTrue(ticks.values.none { it == 0.0 && 1.0 / it < 0 }, "found -0.0 in ${ticks.values}")
    }

    @Test
    fun rejectsBadInput() {
        assertFailsWith<IllegalArgumentException> { NiceScale.ticks(Double.NaN, 1.0) }
        assertFailsWith<IllegalArgumentException> { NiceScale.ticks(0.0, 1.0, targetCount = 1) }
        assertFailsWith<IllegalArgumentException> { NiceScale.niceNumber(0.0, round = true) }
    }
}
