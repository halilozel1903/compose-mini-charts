package io.github.halilozel1903.minicharts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.minicharts.core.BarMode

private val previewMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val previewRevenue = listOf(3.2, 4.1, 3.9, 5.2, 6.1, 7.3, 9.8, 8.9, 7.7, 8.1, 9.1, 9.4).map { it * 1_000 }
private val previewDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
private fun PreviewCharts() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Sparkline(previewRevenue, Modifier.size(120.dp, 36.dp), title = "Revenue")
        LineChart(
            series = listOf(
                LineSeries("This year", previewRevenue),
                LineSeries("Last year", previewRevenue.map { it * 0.7 }, fill = false, dashed = true),
            ),
            labels = previewMonths,
            title = "Revenue",
            initialHighlight = 6,
            animationSpec = null,
            modifier = Modifier.fillMaxWidth().height(220.dp),
        )
        BarChart(
            series = listOf(
                BarSeries("Run", listOf(3.1, 0.0, 4.2, 2.0, 0.0, 8.4, 5.0)),
                BarSeries("Walk", listOf(4.0, 5.5, 3.1, 6.2, 4.4, 3.0, 6.1)),
            ),
            labels = previewDays,
            title = "Distance",
            mode = BarMode.Stacked,
            initialHighlight = 5,
            animationSpec = null,
            modifier = Modifier.fillMaxWidth().height(200.dp),
        )
        DonutChart(
            slices = listOf(DonutSlice("Rent", 1_008), DonutSlice("Food", 600), DonutSlice("Travel", 432), DonutSlice("Other", 360)),
            title = "Spending",
            legendPosition = LegendPosition.End,
            animationSpec = null,
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
    }
}

@Preview(name = "Charts", widthDp = 380, heightDp = 860)
@Composable
private fun ChartsPreview() {
    MaterialTheme {
        Surface { PreviewCharts() }
    }
}

@Preview(name = "Charts, dark", widthDp = 380, heightDp = 860)
@Composable
private fun ChartsDarkPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface { PreviewCharts() }
    }
}
