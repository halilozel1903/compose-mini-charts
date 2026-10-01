package io.github.halilozel1903.minicharts.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.minicharts.BarChart
import io.github.halilozel1903.minicharts.BarSeries
import io.github.halilozel1903.minicharts.ChartDefaults
import io.github.halilozel1903.minicharts.DonutChart
import io.github.halilozel1903.minicharts.DonutSlice
import io.github.halilozel1903.minicharts.LegendPosition
import io.github.halilozel1903.minicharts.LineChart
import io.github.halilozel1903.minicharts.LineSeries
import io.github.halilozel1903.minicharts.Sparkline
import io.github.halilozel1903.minicharts.core.BarMode
import io.github.halilozel1903.minicharts.core.LineCurve

/** Green for gains, readable on light and dark surfaces. */
@Composable
private fun positiveColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) Color(0xFF7BE0A6) else Color(0xFF16834A)

@Composable
fun ChartCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (trailing != null) {
                    Text(trailing, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun BalanceCard() {
    val scheme = MaterialTheme.colorScheme
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = scheme.primary, contentColor = scheme.onPrimary) {
        Column(Modifier.padding(20.dp)) {
            Text("Total balance", style = MaterialTheme.typography.labelLarge, color = scheme.onPrimary.copy(alpha = 0.8f))
            Text("$24,830.50", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("+4.2% this month", style = MaterialTheme.typography.bodyMedium, color = scheme.onPrimary.copy(alpha = 0.85f))
            Spacer(Modifier.height(12.dp))
            Sparkline(
                values = SampleData.balance,
                color = scheme.onPrimary,
                strokeWidth = 2.5.dp,
                title = "Balance, last 30 days",
                valueFormatter = SampleData.dollars,
                modifier = Modifier.fillMaxWidth().height(64.dp),
            )
        }
    }
}

@Composable
fun KpiRow() {
    val palette = ChartDefaults.seriesColors()
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KpiTile("Income", "$8.4k", "+12%", SampleData.income, palette[3], up = true, modifier = Modifier.weight(1f))
        KpiTile("Spending", "$3.1k", "-8%", SampleData.spending, palette[1], up = false, modifier = Modifier.weight(1f))
        KpiTile("Steps", "9.1k", "+7%", SampleData.stepsTrend, palette[2], up = true, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun KpiTile(label: String, value: String, change: String, data: List<Number>, color: Color, up: Boolean, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                change,
                style = MaterialTheme.typography.labelMedium,
                color = if (up) positiveColor() else MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.height(8.dp))
            Sparkline(values = data, color = color, title = label, modifier = Modifier.fillMaxWidth().height(36.dp))
        }
    }
}

@Composable
fun RevenueCard() {
    ChartCard(title = "Revenue", subtitle = "2026, by month", trailing = "$82.8k") {
        LineChart(
            values = SampleData.revenue,
            labels = SampleData.months,
            title = "Revenue",
            valueFormatter = SampleData.dollars,
            initialHighlight = 6,
            modifier = Modifier.fillMaxWidth().height(220.dp),
        )
    }
}

@Composable
fun SpendingCard(big: Boolean) {
    val slices = SampleData.spendingLabels.zip(SampleData.spendingValues) { label, value -> DonutSlice(label, value) }
    ChartCard(title = "Spending", subtitle = "September", trailing = "$2,470") {
        DonutChart(
            slices = slices,
            title = "Spending",
            centerLabel = "this month",
            valueFormatter = SampleData.dollars,
            legendPosition = if (big) LegendPosition.Bottom else LegendPosition.End,
            initialHighlight = if (big) 0 else null,
            modifier = Modifier.fillMaxWidth().height(if (big) 340.dp else 180.dp),
        )
    }
}

@Composable
fun StepsCard() {
    ChartCard(title = "Weekly steps", subtitle = "Goal 10k a day", trailing = "52.4k") {
        BarChart(
            values = SampleData.steps,
            labels = SampleData.days,
            title = "Weekly steps",
            initialHighlight = 5,
            modifier = Modifier.fillMaxWidth().height(200.dp),
        )
    }
}

@Composable
fun RevenueCompareCard() {
    ChartCard(title = "Revenue", subtitle = "This year against last year", trailing = "+31%") {
        LineChart(
            series = listOf(
                LineSeries("This year", SampleData.revenue),
                LineSeries("Last year", SampleData.revenueLastYear, fill = false, dashed = true, strokeWidth = 2.dp),
            ),
            labels = SampleData.months,
            title = "Revenue",
            valueFormatter = SampleData.dollars,
            initialHighlight = 6,
            modifier = Modifier.fillMaxWidth().height(280.dp),
        )
    }
}

@Composable
fun CashFlowCard() {
    ChartCard(title = "Net cash flow", subtitle = "Straight lines, points and zero on the axis") {
        LineChart(
            series = listOf(LineSeries("Cash flow", SampleData.cashFlow, fill = false, curve = LineCurve.Straight)),
            labels = SampleData.months,
            title = "Net cash flow",
            valueFormatter = SampleData.dollars,
            includeZero = true,
            showPoints = true,
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
    }
}

@Composable
fun WatchlistCard() {
    val palette = ChartDefaults.seriesColors()
    val rows: List<Triple<String, String, List<Number>>> = listOf(
        Triple("Index fund", "+6.1%", listOf(100, 102, 101, 104, 106, 105, 108, 110, 109, 112)),
        Triple("Bonds", "+0.8%", listOf(50, 50.2, 50.1, 50.3, 50.2, 50.4, 50.3, 50.5, 50.4, 50.4)),
        Triple("Gold", "-2.4%", listOf(80, 82, 81, 79, 80, 78, 77, 78, 76, 78)),
    )
    ChartCard(title = "Watchlist", subtitle = "Sparklines in a list") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEachIndexed { index, (name, change, data) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Sparkline(
                        values = data,
                        color = palette[index],
                        highlightMinMax = true,
                        title = name,
                        modifier = Modifier.size(width = 96.dp, height = 28.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        change,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (change.startsWith("+")) positiveColor() else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.width(52.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityCard() {
    ChartCard(title = "Active minutes", subtitle = "Grouped bars") {
        BarChart(
            series = listOf(
                BarSeries("Run", SampleData.run),
                BarSeries("Walk", SampleData.walk),
                BarSeries("Cycle", SampleData.cycle),
            ),
            labels = SampleData.days,
            title = "Active minutes",
            valueFormatter = SampleData.minutes,
            barSpacing = 2.dp,
            cornerRadius = 3.dp,
            groupSpacing = 0.25f,
            modifier = Modifier.fillMaxWidth().height(220.dp),
        )
    }
}

@Composable
fun MonthlySpendingCard() {
    ChartCard(title = "Monthly spending", subtitle = "Stacked bars", trailing = "$10.2k") {
        BarChart(
            series = listOf(
                BarSeries("Bills", SampleData.bills),
                BarSeries("Food", SampleData.food),
                BarSeries("Travel", SampleData.travel),
            ),
            labels = SampleData.halfYear,
            title = "Monthly spending",
            mode = BarMode.Stacked,
            valueFormatter = SampleData.dollars,
            initialHighlight = 2,
            modifier = Modifier.fillMaxWidth().height(260.dp),
        )
    }
}

@Composable
fun MacrosCard() {
    val slices = SampleData.macroLabels.zip(SampleData.macroValues) { label, value -> DonutSlice(label, value) }
    ChartCard(title = "Macros", subtitle = "Today", trailing = "2,070 kcal") {
        DonutChart(
            slices = slices,
            title = "Macros",
            centerLabel = "eaten",
            valueFormatter = SampleData.grams,
            legendPosition = LegendPosition.End,
            gapDegrees = 3f,
            modifier = Modifier.fillMaxWidth().height(170.dp),
        )
    }
}

@Composable
fun GoalsCard() {
    val scheme = MaterialTheme.colorScheme
    ChartCard(title = "Goals", subtitle = "A donut as a progress ring") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GoalRing("Steps", 72, scheme.primary, Modifier.weight(1f))
            GoalRing("Savings", 45, ChartDefaults.seriesColors()[3], Modifier.weight(1f))
        }
    }
}

@Composable
private fun GoalRing(label: String, percent: Int, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        DonutChart(
            slices = listOf(
                DonutSlice("Done", percent, color),
                DonutSlice("Left", 100 - percent, MaterialTheme.colorScheme.surfaceVariant),
            ),
            title = "$label goal",
            centerValue = "$percent%",
            centerLabel = label,
            gapDegrees = 0f,
            legendPosition = LegendPosition.None,
            highlightEnabled = false,
            modifier = Modifier.fillMaxWidth().height(130.dp),
        )
    }
}
