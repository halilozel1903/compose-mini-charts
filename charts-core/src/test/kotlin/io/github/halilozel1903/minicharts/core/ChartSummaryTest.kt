package io.github.halilozel1903.minicharts.core

import kotlin.test.Test
import kotlin.test.assertEquals

class ChartSummaryTest {
    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "July", "Aug", "Sep", "Oct", "Nov", "Dec")
    private val revenue = listOf(3_200.0, 4_100.0, 3_900.0, 5_200.0, 6_100.0, 7_300.0, 9_800.0, 8_900.0, 7_700.0, 8_100.0, 9_100.0, 9_400.0)

    @Test
    fun lineSummaryFromTheReadme() {
        assertEquals("Revenue, 12 points, from 3.2k to 9.8k, highest in July", ChartSummary.line("Revenue", revenue, months))
    }

    @Test
    fun lineWithoutLabels() {
        assertEquals("Steps, 3 points, from 1 to 3, highest at point 2", ChartSummary.line("Steps", listOf(1.0, 3.0, 2.0)))
    }

    @Test
    fun lineEdgeCases() {
        assertEquals("Revenue, no data", ChartSummary.line("Revenue", emptyList()))
        assertEquals("Revenue, 1 point, 5k", ChartSummary.line("Revenue", listOf(5_000.0)))
        assertEquals("Revenue, 3 points, all 2", ChartSummary.line("Revenue", listOf(2.0, 2.0, 2.0)))
    }

    @Test
    fun severalLines() {
        val text = ChartSummary.lines(
            "Revenue",
            listOf(SeriesData("This year", revenue), SeriesData("Last year", revenue.map { it / 2 })),
            months,
        )
        assertEquals(
            "Revenue, 2 series, 12 points. This year from 3.2k to 9.8k, highest in July. " +
                "Last year from 1.6k to 4.9k, highest in July",
            text,
        )
    }

    @Test
    fun singleSeriesBars() {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val steps = listOf(6_200.0, 4_200.0, 8_000.0, 7_100.0, 9_300.0, 11_800.0, 5_400.0)
        assertEquals(
            "Weekly steps, 7 bars, from 4.2k to 11.8k, tallest Sat",
            ChartSummary.bars("Weekly steps", steps.map { listOf(it) }, days),
        )
    }

    @Test
    fun groupedAndStackedBars() {
        val data = listOf(listOf(1_100.0, 3_000.0), listOf(2_000.0, 9_000.0))
        assertEquals(
            "Activity, 2 groups of Run and Walk, from 1.1k to 9k, tallest Sat Walk",
            ChartSummary.bars("Activity", data, listOf("Fri", "Sat"), listOf("Run", "Walk")),
        )
        assertEquals(
            "Activity, 2 stacked bars of Run and Walk, totals from 4.1k to 11k, tallest Sat",
            ChartSummary.bars("Activity", data, listOf("Fri", "Sat"), listOf("Run", "Walk"), BarMode.Stacked),
        )
    }

    @Test
    fun donutSummary() {
        assertEquals(
            "Spending, 4 slices, total 2.4k: Rent 42%, Food 25%, Travel 18%, Other 15%",
            ChartSummary.donut("Spending", listOf("Rent", "Food", "Travel", "Other"), listOf(1_008.0, 600.0, 432.0, 360.0)),
        )
        assertEquals("Spending, no data", ChartSummary.donut("Spending", listOf("Rent"), listOf(0.0)))
    }
}
