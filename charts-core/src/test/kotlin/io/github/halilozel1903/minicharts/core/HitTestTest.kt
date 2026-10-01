package io.github.halilozel1903.minicharts.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HitTestTest {

    @Test
    fun nearestIndexByX() {
        val xs = listOf(0f, 10f, 20f, 30f)
        assertEquals(0, HitTest.nearestIndex(xs, -5f))
        assertEquals(1, HitTest.nearestIndex(xs, 12f))
        assertEquals(2, HitTest.nearestIndex(xs, 16f))
        assertEquals(1, HitTest.nearestIndex(xs, 15f)) // tie: left wins
        assertEquals(3, HitTest.nearestIndex(xs, 99f))
        assertEquals(-1, HitTest.nearestIndex(emptyList(), 3f))
        assertEquals(0, HitTest.nearestIndex(listOf(5f), 300f))
    }

    @Test
    fun nearestPointWithinDistance() {
        val points = listOf(ChartPoint(0f, 0f), ChartPoint(10f, 10f), ChartPoint(20f, 0f))
        assertEquals(1, HitTest.nearestPoint(points, 11f, 9f))
        assertNull(HitTest.nearestPoint(points, 50f, 50f, maxDistance = 5f))
    }

    @Test
    fun barAtWithSlop() {
        val rects = listOf(BarRect(0, 0, 5.0, 10f, 10f, 12f, 50f))
        assertNull(HitTest.barAt(rects, 15f, 30f))
        assertEquals(rects[0], HitTest.barAt(rects, 15f, 30f, slop = 4f))
    }

    @Test
    fun groupAtFallsBackToNearest() {
        val slots = listOf(Span(0f, 10f), Span(10f, 20f))
        assertEquals(1, HitTest.groupAt(slots, 14f))
        assertEquals(1, HitTest.groupAt(slots, 40f))
        assertEquals(-1, HitTest.groupAt(emptyList(), 1f))
    }
}
