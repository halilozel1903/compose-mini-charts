package io.github.halilozel1903.minicharts.core

/** A named list of values, such as one line of a line chart. */
public data class SeriesData(val name: String, val values: List<Double>)

/**
 * One sentence descriptions of charts for screen readers, for example
 * "Revenue, 12 points, from 3.2k to 9.8k, highest in July".
 * The charts use them as their content description.
 */
public object ChartSummary {

    /** A line or sparkline: "Revenue, 12 points, from 3.2k to 9.8k, highest in July". */
    public fun line(
        title: String,
        values: List<Double>,
        labels: List<String> = emptyList(),
        formatter: ValueFormatter = ValueFormatter.Compact,
    ): String = "$title, " + describe(values, labels, formatter, unit = "point", peak = this::highestIn)

    /**
     * A line chart with several series. One series reads like [line]; more read
     * "Revenue, 2 series, 12 points. This year from 3.2k to 9.8k, highest in July. Last year from ...".
     */
    public fun lines(
        title: String,
        series: List<SeriesData>,
        labels: List<String> = emptyList(),
        formatter: ValueFormatter = ValueFormatter.Compact,
    ): String {
        if (series.size == 1) return line(title, series[0].values, labels, formatter)
        if (series.isEmpty() || series.all { it.values.isEmpty() }) return "$title, no data"
        val points = series.maxOf { it.values.size }
        val parts = series.joinToString(". ") { item ->
            "${item.name} " + rangeAndPeak(item.values, labels, formatter, this::highestIn)
        }
        return "$title, ${series.size} series, ${count(points, "point")}. $parts"
    }

    /**
     * A bar chart, data indexed as `data[group][series]`.
     * One series: "Weekly steps, 7 bars, from 4.2k to 11.8k, tallest Sat".
     * Grouped: "Activity, 7 groups of Run and Walk, from 1.1k to 9k, tallest Sat Walk".
     * Stacked: "Spending, 6 stacked bars of Food and Travel, totals from 1.2k to 3.4k, tallest Mar".
     */
    public fun bars(
        title: String,
        data: List<List<Double>>,
        groupLabels: List<String> = emptyList(),
        seriesNames: List<String> = emptyList(),
        mode: BarMode = BarMode.Grouped,
        formatter: ValueFormatter = ValueFormatter.Compact,
    ): String {
        if (data.isEmpty() || data.all { it.isEmpty() }) return "$title, no data"
        val seriesCount = data.maxOf { it.size }
        if (seriesCount <= 1) {
            return "$title, " + describe(data.map { it.firstOrNull() ?: 0.0 }, groupLabels, formatter, "bar", this::tallest)
        }
        val names = List(seriesCount) { seriesNames.getOrNull(it) ?: "series ${it + 1}" }
        val nameList = joinNames(names)
        return if (mode == BarMode.Stacked) {
            val totals = data.map { group -> group.filter { it.isFinite() }.sum() }
            "$title, ${count(data.size, "stacked bar")} of $nameList, totals " +
                rangeAndPeak(totals, groupLabels, formatter, this::tallest)
        } else {
            val flat = data.flatMap { it.map { value -> if (value.isFinite()) value else 0.0 } }
            val range = ValueRange.of(flat) ?: return "$title, no data"
            var peakGroup = 0
            var peakSeries = 0
            search@ for (g in data.indices) {
                for (s in data[g].indices) {
                    if (data[g][s] == range.max) {
                        peakGroup = g
                        peakSeries = s
                        break@search
                    }
                }
            }
            val where = (groupLabels.getOrNull(peakGroup) ?: "group ${peakGroup + 1}") + " " + names[peakSeries]
            "$title, ${count(data.size, "group")} of $nameList, from ${formatter.format(range.min)} to " +
                "${formatter.format(range.max)}, tallest $where"
        }
    }

    /** A donut: "Spending, 4 slices, total 2.4k: Rent 42%, Food 25%, Travel 18%, Other 15%". */
    public fun donut(
        title: String,
        labels: List<String>,
        values: List<Double>,
        formatter: ValueFormatter = ValueFormatter.Compact,
        percentDecimals: Int = 0,
    ): String {
        val total = values.filter { it.isFinite() && it > 0.0 }.sum()
        if (values.isEmpty() || total <= 0.0) return "$title, no data"
        val shares = ChartFormat.percentages(values, percentDecimals)
        val parts = values.indices.joinToString(", ") { index ->
            val label = labels.getOrNull(index) ?: "slice ${index + 1}"
            "$label ${ChartFormat.number(shares[index], percentDecimals)}%"
        }
        return "$title, ${count(values.size, "slice")}, total ${formatter.format(total)}: $parts"
    }

    /** "12 points, from 3.2k to 9.8k, highest in July", or the short forms for empty, single and flat data. */
    private fun describe(
        values: List<Double>,
        labels: List<String>,
        formatter: ValueFormatter,
        unit: String,
        peak: (label: String?, index: Int) -> String,
    ): String {
        val finite = values.filter { it.isFinite() }
        if (finite.isEmpty()) return "no data"
        if (values.size == 1) return "${count(1, unit)}, ${formatter.format(finite[0])}"
        return "${count(values.size, unit)}, " + rangeAndPeak(values, labels, formatter, peak)
    }

    private fun rangeAndPeak(
        values: List<Double>,
        labels: List<String>,
        formatter: ValueFormatter,
        peak: (label: String?, index: Int) -> String,
    ): String {
        val range = ValueRange.of(values) ?: return "no data"
        if (range.span == 0.0) return "all ${formatter.format(range.min)}"
        val index = values.indexOfFirst { it == range.max }
        return "from ${formatter.format(range.min)} to ${formatter.format(range.max)}, ${peak(labels.getOrNull(index), index)}"
    }

    private fun highestIn(label: String?, index: Int): String =
        if (label != null) "highest in $label" else "highest at point ${index + 1}"

    private fun tallest(label: String?, index: Int): String =
        if (label != null) "tallest $label" else "tallest is bar ${index + 1}"

    private fun count(n: Int, unit: String): String = if (n == 1) "1 $unit" else "$n ${unit}s"

    private fun joinNames(names: List<String>): String = when (names.size) {
        0 -> ""
        1 -> names[0]
        else -> names.dropLast(1).joinToString(", ") + " and " + names.last()
    }
}
