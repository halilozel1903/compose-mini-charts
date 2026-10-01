package io.github.halilozel1903.minicharts.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import io.github.halilozel1903.minicharts.LocalChartAnimationsEnabled

/**
 * A finance and fitness dashboard built from the charts. `scripts/screenshots.sh` starts it with
 * `--es scene <scene>` to show a fixed screen for README screenshots:
 *
 * - `dashboard`: balance, KPIs, revenue, spending and steps (two columns on tablets)
 * - `line`: line charts, with July highlighted
 * - `bars`: grouped, stacked and single bar charts, with Saturday highlighted
 * - `donut`: donuts with the first slice highlighted
 *
 * With a scene, entry animations are off so every chart is complete in the first frame. Highlights are
 * preselected in every mode, so captures are always the same.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))
        setContent {
            SampleTheme(dark = isSystemInDarkTheme()) {
                // Screenshot scenes show every chart in its final state at once.
                CompositionLocalProvider(LocalChartAnimationsEnabled provides (scene == null)) {
                    // The Surface makes text default to onBackground, so it stays readable in dark mode.
                    Surface(color = MaterialTheme.colorScheme.background) {
                        SampleApp(scene)
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
    }
}
