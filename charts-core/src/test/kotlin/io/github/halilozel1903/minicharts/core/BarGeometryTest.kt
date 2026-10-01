package io.github.halilozel1903.minicharts.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BarGeometryTest {
    private val area = PlotArea(0f, 0f, 400f, 100f)

    @Test
    fun groupSpansLeaveSpacing() {
        val spans = BarGeometry.groupSpans(4, area, groupSpacing = 0.5f)
        assertEquals(Span(25f, 75f), spans[0])
        assertEquals(Span(325f, 375f), spans[3])
    }

    @Test
    fun groupedBarsSitSideBySideWithGaps() {
        val rects = BarGeometry.layout(
            data = listOf(listOf(50.0, 100.0)),
            area = PlotArea(0f, 0f, 100f, 100f),
            yMin = 0.0,
            yMax = 100.0,
            groupSpacing = 0f,
            barSpacing = 10f,
        )
        assertEquals(2, rects.size)
        assertEquals(0f, rects[0].left)
        assertEquals(45f, rects[0].right)
        assertEquals(55f, rects[1].left)
        assertEquals(100f, rects[1].right)
        assertEquals(50f, rects[0].top)
        assertEquals(0f, rects[1].top)
        assertTrue(rects.all { it.bottom == 100f })
    }

    @Test
    fun stackedBarsStartWhereThePreviousEnds() {
        val rects = BarGeometry.layout(listOf(listOf(20.0, 30.0, 50.0)), area, 0.0, 100.0, mode = BarMode.Stacked)
        assertEquals(listOf(100f, 80f, 50f), rects.map { it.bottom })
        assertEquals(listOf(80f, 50f, 0f), rects.map { it.top })
    }

    @Test
    fun stackSpacingInsetsUpperSegments() {
        val rects = BarGeometry.layout(listOf(listOf(50.0, 50.0)), area, 0.0, 100.0, BarMode.Stacked, stackSpacing = 2f)
        assertEquals(100f, rects[0].bottom)
        assertEquals(48f, rects[1].bottom)
    }

    @Test
    fun negativeValuesHangBelowZero() {
        val rects = BarGeometry.layout(listOf(listOf(-50.0), listOf(50.0)), area, -100.0, 100.0)
        assertEquals(50f, rects[0].top)
        assertEquals(75f, rects[0].bottom)
        assertEquals(25f, rects[1].top)
        assertEquals(50f, rects[1].bottom)
    }

    @Test
    fun stackedNegativesGoDownwards() {
        val rects = BarGeometry.layout(listOf(listOf(40.0, -20.0, -20.0)), area, -50.0, 50.0, BarMode.Stacked)
        assertEquals(50f, rects[1].top)
        assertEquals(70f, rects[1].bottom)
        assertEquals(70f, rects[2].top)
        assertEquals(90f, rects[2].bottom)
    }

    @Test
    fun valueRangeContainsZeroAndStackTotals() {
        assertEquals(ValueRange(0.0, 9.0), BarGeometry.valueRange(listOf(listOf(4.0, 9.0), listOf(2.0)), BarMode.Grouped))
        assertEquals(ValueRange(-5.0, 13.0), BarGeometry.valueRange(listOf(listOf(4.0, 9.0, -5.0), listOf(2.0)), BarMode.Stacked))
    }

    @Test
    fun zeroValuesStillGetARect() {
        val rects = BarGeometry.layout(listOf(listOf(0.0, Double.NaN)), area, 0.0, 10.0)
        assertEquals(2, rects.size)
        assertTrue(rects.all { it.height == 0f })
    }

    @Test
    fun topSegmentOfEachStack() {
        assertEquals(listOf(1, -1, 0), BarGeometry.topSeriesOfStacks(listOf(listOf(1.0, 2.0, 0.0), listOf(0.0), listOf(3.0, -1.0))))
    }
}
