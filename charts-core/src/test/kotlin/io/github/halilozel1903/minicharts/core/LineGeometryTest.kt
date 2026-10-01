package io.github.halilozel1903.minicharts.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LineGeometryTest {
    private val area = PlotArea(0f, 0f, 300f, 100f)

    @Test
    fun pointsAreEvenlySpacedAndScaled() {
        val points = LineGeometry.points(listOf(0.0, 50.0, 100.0, 25.0), area, yMin = 0.0, yMax = 100.0)
        assertEquals(listOf(0f, 100f, 200f, 300f), points.map { it.x })
        assertEquals(listOf(100f, 50f, 0f, 75f), points.map { it.y })
    }

    @Test
    fun singlePointIsCentered() {
        assertEquals(listOf(150f), LineGeometry.xPositions(1, area))
        assertEquals(emptyList(), LineGeometry.xPositions(0, area))
    }

    @Test
    fun shorterSeriesUseTheSameSlots() {
        val points = LineGeometry.points(listOf(1.0, 2.0), area, 0.0, 2.0, slots = 4)
        assertEquals(listOf(0f, 100f), points.map { it.x })
    }

    @Test
    fun straightSegments() {
        val points = listOf(ChartPoint(0f, 0f), ChartPoint(10f, 5f), ChartPoint(20f, 0f))
        val segments = LineGeometry.segments(points, LineCurve.Straight)
        assertIs<PathSegment.MoveTo>(segments[0])
        assertEquals(listOf(PathSegment.LineTo(points[1]), PathSegment.LineTo(points[2])), segments.drop(1))
    }

    @Test
    fun smoothCurvePassesThroughEveryPoint() {
        val points = LineGeometry.points(listOf(3.0, 7.0, 4.0, 9.0, 8.0), area, 0.0, 10.0)
        val segments = LineGeometry.segments(points, LineCurve.Smooth)
        assertEquals(points.size, segments.size)
        assertEquals(points, segments.map { it.end })
        assertTrue(segments.drop(1).all { it is PathSegment.CubicTo })
    }

    @Test
    fun smoothCurveNeverOvershoots() {
        // Control points inside the band of their segment keep the whole cubic inside it.
        val values = listOf(1.0, 1.2, 9.0, 9.1, 2.0, 2.0, 8.0, 3.0, 3.5, 10.0)
        val points = LineGeometry.points(values, area, 0.0, 10.0)
        val segments = LineGeometry.segments(points, LineCurve.Smooth)
        segments.drop(1).forEachIndexed { index, segment ->
            segment as PathSegment.CubicTo
            val low = minOf(points[index].y, points[index + 1].y) - 1e-3f
            val high = maxOf(points[index].y, points[index + 1].y) + 1e-3f
            assertTrue(segment.control1.y in low..high, "segment $index control1 ${segment.control1} outside $low..$high")
            assertTrue(segment.control2.y in low..high, "segment $index control2 ${segment.control2} outside $low..$high")
        }
    }

    @Test
    fun localExtremaGetFlatTangents() {
        val points = listOf(ChartPoint(0f, 10f), ChartPoint(10f, 0f), ChartPoint(20f, 10f))
        val tangents = LineGeometry.monotoneTangents(points)
        assertEquals(0.0, tangents[1])
    }

    @Test
    fun twoPointsAreAStraightLine() {
        val points = listOf(ChartPoint(0f, 0f), ChartPoint(10f, 10f))
        assertIs<PathSegment.LineTo>(LineGeometry.segments(points, LineCurve.Smooth)[1])
    }
}
