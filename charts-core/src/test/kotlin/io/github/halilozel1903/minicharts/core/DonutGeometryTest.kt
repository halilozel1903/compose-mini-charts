package io.github.halilozel1903.minicharts.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DonutGeometryTest {

    @Test
    fun arcsWithoutGapsFillTheCircle() {
        val arcs = DonutGeometry.arcs(listOf(1.0, 1.0, 2.0))
        assertEquals(listOf(90f, 90f, 180f), arcs.map { it.sweepAngle })
        assertEquals(-90f, arcs[0].startAngle)
        assertEquals(0f, arcs[1].startAngle)
        assertEquals(270f, arcs[2].endAngle)
        assertEquals(listOf(0.25, 0.25, 0.5), arcs.map { it.fraction })
    }

    @Test
    fun gapsAreLeftBetweenSlices() {
        val arcs = DonutGeometry.arcs(listOf(1.0, 1.0, 1.0, 1.0), startAngle = 0f, gapDegrees = 4f)
        assertEquals(List(4) { 86f }, arcs.map { it.sweepAngle })
        assertEquals(2f, arcs[0].startAngle)
        arcs.zipWithNext { a, b -> assertEquals(4f, b.startAngle - a.endAngle, 1e-4f) }
        val total = arcs.sumOf { it.sweepAngle.toDouble() } + 4 * 4.0
        assertEquals(360.0, total, 1e-3)
    }

    @Test
    fun singleSliceHasNoGap() {
        val arcs = DonutGeometry.arcs(listOf(5.0), gapDegrees = 10f)
        assertEquals(360f, arcs.single().sweepAngle)
    }

    @Test
    fun zeroAndNegativeValuesGetEmptyArcs() {
        val arcs = DonutGeometry.arcs(listOf(3.0, 0.0, -2.0, 1.0), gapDegrees = 2f)
        assertEquals(0f, arcs[1].sweepAngle)
        assertEquals(0f, arcs[2].sweepAngle)
        assertEquals(0.75, arcs[0].fraction)
        assertEquals(-2.0, arcs[2].value)
        assertEquals(356f, arcs[0].sweepAngle + arcs[3].sweepAngle, 1e-3f)
    }

    @Test
    fun hugeGapsAreCapped() {
        val arcs = DonutGeometry.arcs(listOf(1.0, 1.0), gapDegrees = 400f)
        assertEquals(listOf(90f, 90f), arcs.map { it.sweepAngle })
    }

    @Test
    fun allZeroGivesEmptyArcs() {
        val arcs = DonutGeometry.arcs(listOf(0.0, 0.0))
        assertEquals(listOf(0f, 0f), arcs.map { it.sweepAngle })
    }

    @Test
    fun angleOfUsesCanvasDirections() {
        assertEquals(0f, DonutGeometry.angleOf(1f, 0f))
        assertEquals(90f, DonutGeometry.angleOf(0f, 1f))
        assertEquals(180f, DonutGeometry.angleOf(-1f, 0f))
        assertEquals(270f, DonutGeometry.angleOf(0f, -1f))
    }

    @Test
    fun hitTestFindsSliceAndIgnoresHoleAndGaps() {
        // Starts at twelve o'clock: slice 0 covers the right half, slice 1 the left half.
        val arcs = DonutGeometry.arcs(listOf(1.0, 1.0), gapDegrees = 10f)
        assertEquals(0, DonutGeometry.hitTest(arcs, 80f, 0f, innerRadius = 50f, outerRadius = 100f))
        assertEquals(1, DonutGeometry.hitTest(arcs, -80f, 0f, innerRadius = 50f, outerRadius = 100f))
        assertNull(DonutGeometry.hitTest(arcs, 10f, 0f, innerRadius = 50f, outerRadius = 100f))
        assertNull(DonutGeometry.hitTest(arcs, 200f, 0f, innerRadius = 50f, outerRadius = 100f))
        assertNull(DonutGeometry.hitTest(arcs, 0f, -80f, innerRadius = 50f, outerRadius = 100f))
    }

    @Test
    fun containsAngleWrapsAround() {
        val arc = DonutArc(0, 1.0, 1.0, startAngle = 300f, sweepAngle = 120f)
        assertEquals(true, arc.containsAngle(10f))
        assertEquals(true, arc.containsAngle(-30f))
        assertEquals(false, arc.containsAngle(90f))
    }
}
