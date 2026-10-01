package io.github.halilozel1903.minicharts.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScalingTest {

    @Test
    fun verticalScaleMapsMinToBottomAndMaxToTop() {
        val area = PlotArea(0f, 10f, 100f, 110f)
        val scale = LinearScale.vertical(0.0, 50.0, area)
        assertEquals(110f, scale.map(0.0))
        assertEquals(10f, scale.map(50.0))
        assertEquals(60f, scale.map(25.0))
        assertEquals(25.0, scale.invert(60f), 1e-9)
    }

    @Test
    fun emptyDomainMapsToTheMiddle() {
        val scale = LinearScale(3.0, 3.0, 0f, 100f)
        assertEquals(50f, scale.map(3.0))
    }

    @Test
    fun normalizeToUnitRange() {
        assertEquals(listOf(0.0, 0.5, 1.0), ChartData.normalize(listOf(10.0, 15.0, 20.0)))
        assertEquals(listOf(0.5, 0.5), ChartData.normalize(listOf(7.0, 7.0)))
        assertEquals(emptyList(), ChartData.normalize(emptyList()))
    }

    @Test
    fun valueRangeSkipsNonFiniteValues() {
        assertEquals(ValueRange(-2.0, 8.0), ValueRange.of(listOf(3.0, Double.NaN, -2.0, 8.0, Double.POSITIVE_INFINITY)))
        assertNull(ValueRange.of(listOf(Double.NaN)))
    }

    @Test
    fun sanitizeAcceptsAnyNumber() {
        assertEquals(listOf(1.0, 2.5, 0.0, 4.0), ChartData.sanitize(listOf(1, 2.5f, Double.NaN, 4L)))
    }
}
