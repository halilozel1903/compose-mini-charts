package io.github.halilozel1903.minicharts

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle

/**
 * The colors of a chart. [ChartDefaults.colors] builds them from the Material 3 theme, so charts follow
 * light and dark mode and dynamic color.
 */
@Immutable
public class ChartColors(
    /** Colors for series and slices, used in order and repeated when there are more series. */
    public val series: List<Color>,
    /** Horizontal grid lines. */
    public val grid: Color,
    /** Axis tick and category labels. */
    public val axisLabel: Color,
    /** The vertical guide line and band behind a highlighted point or bar group. */
    public val highlight: Color,
    /** Tooltip background. */
    public val tooltipContainer: Color,
    /** Tooltip text. */
    public val tooltipContent: Color,
    /** What the chart sits on; used for the hole of highlighted points. */
    public val background: Color,
    /** The empty ring of a donut without data. */
    public val track: Color,
    /** The big value in the middle of a donut. */
    public val centerValue: Color,
    /** The caption under it. */
    public val centerLabel: Color,
) {
    /** The color of series [index], cycling through [series]. */
    public fun seriesColor(index: Int): Color =
        if (series.isEmpty()) axisLabel else series[Math.floorMod(index, series.size)]

    /** [color] when it is specified, otherwise the color of series [index]. */
    public fun resolve(color: Color, index: Int): Color = if (color.isSpecified) color else seriesColor(index)

    /** A copy with some colors replaced. */
    public fun copy(
        series: List<Color> = this.series,
        grid: Color = this.grid,
        axisLabel: Color = this.axisLabel,
        highlight: Color = this.highlight,
        tooltipContainer: Color = this.tooltipContainer,
        tooltipContent: Color = this.tooltipContent,
        background: Color = this.background,
        track: Color = this.track,
        centerValue: Color = this.centerValue,
        centerLabel: Color = this.centerLabel,
    ): ChartColors = ChartColors(
        series, grid, axisLabel, highlight, tooltipContainer, tooltipContent, background, track, centerValue, centerLabel,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChartColors) return false
        return series == other.series && grid == other.grid && axisLabel == other.axisLabel &&
            highlight == other.highlight && tooltipContainer == other.tooltipContainer &&
            tooltipContent == other.tooltipContent && background == other.background && track == other.track &&
            centerValue == other.centerValue && centerLabel == other.centerLabel
    }

    override fun hashCode(): Int {
        var result = series.hashCode()
        result = 31 * result + grid.hashCode()
        result = 31 * result + axisLabel.hashCode()
        result = 31 * result + highlight.hashCode()
        result = 31 * result + tooltipContainer.hashCode()
        result = 31 * result + tooltipContent.hashCode()
        result = 31 * result + background.hashCode()
        result = 31 * result + track.hashCode()
        result = 31 * result + centerValue.hashCode()
        result = 31 * result + centerLabel.hashCode()
        return result
    }
}

/** Default colors, text styles and animation of the charts. */
public object ChartDefaults {

    /** The entry animation: lines draw from left to right, bars grow and donuts unfold. */
    public val EntryAnimation: AnimationSpec<Float> = tween(durationMillis = 900, easing = FastOutSlowInEasing)

    /**
     * Series colors from the theme: primary and tertiary first, then amber, teal, error and secondary, so up to
     * six series stay apart in light and dark mode.
     */
    @Composable
    public fun seriesColors(): List<Color> {
        val scheme = MaterialTheme.colorScheme
        val dark = scheme.surface.luminance() < 0.5f
        val amber = if (dark) Color(0xFFFFC46B) else Color(0xFFE08A00)
        val teal = if (dark) Color(0xFF6FD8C8) else Color(0xFF00897B)
        return listOf(scheme.primary, scheme.tertiary, amber, teal, scheme.error, scheme.secondary)
    }

    /** Chart colors from the Material 3 theme; pass any of them to override it. */
    @Composable
    public fun colors(
        series: List<Color> = seriesColors(),
        grid: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        axisLabel: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        highlight: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        tooltipContainer: Color = MaterialTheme.colorScheme.inverseSurface,
        tooltipContent: Color = MaterialTheme.colorScheme.inverseOnSurface,
        background: Color = MaterialTheme.colorScheme.surface,
        track: Color = MaterialTheme.colorScheme.surfaceVariant,
        centerValue: Color = MaterialTheme.colorScheme.onSurface,
        centerLabel: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    ): ChartColors = ChartColors(
        series = series,
        grid = grid,
        axisLabel = axisLabel,
        highlight = highlight,
        tooltipContainer = tooltipContainer,
        tooltipContent = tooltipContent,
        background = background,
        track = track,
        centerValue = centerValue,
        centerLabel = centerLabel,
    )

    /** Axis labels: the theme's labelSmall. */
    @Composable
    public fun axisLabelStyle(): TextStyle = MaterialTheme.typography.labelSmall

    /** Tooltip text: the theme's labelMedium. */
    @Composable
    public fun tooltipTextStyle(): TextStyle = MaterialTheme.typography.labelMedium
}

/**
 * Whether charts play their entry animation. Provide `false` to show charts in their final state at once,
 * for example in screenshot tests:
 *
 * ```
 * CompositionLocalProvider(LocalChartAnimationsEnabled provides false) { Dashboard() }
 * ```
 */
public val LocalChartAnimationsEnabled: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { true }
