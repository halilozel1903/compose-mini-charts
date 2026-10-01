package io.github.halilozel1903.minicharts.sample

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Which column a card goes to on tablets. Phones show all cards in one column, in list order. */
enum class Pane { Start, End }

class SampleCard(val pane: Pane, val content: @Composable () -> Unit)

@Composable
fun SampleApp(initialScene: Scene?) {
    var scene by rememberSaveable { mutableStateOf(initialScene ?: Scene.Dashboard) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        val cards = cardsFor(scene)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(horizontal = if (wide) 32.dp else 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 1200.dp).fillMaxWidth()) {
                Header()
                Spacer(Modifier.height(12.dp))
                SceneChips(scene, onSelect = { scene = it })
                Spacer(Modifier.height(16.dp))
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        CardColumn(cards.filter { it.pane == Pane.Start }, Modifier.weight(1f))
                        CardColumn(cards.filter { it.pane == Pane.End }, Modifier.weight(1f))
                    }
                } else {
                    CardColumn(cards, Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CardColumn(cards: List<SampleCard>, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        cards.forEach { it.content() }
    }
}

@Composable
private fun Header() {
    Column {
        Text(
            "Good morning, Ada",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Mini Charts",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SceneChips(selected: Scene, onSelect: (Scene) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Scene.entries.forEach { scene ->
            FilterChip(
                selected = scene == selected,
                onClick = { onSelect(scene) },
                label = { Text(scene.title) },
            )
        }
    }
}

private fun cardsFor(scene: Scene): List<SampleCard> = when (scene) {
    Scene.Dashboard -> listOf(
        SampleCard(Pane.Start) { BalanceCard() },
        SampleCard(Pane.Start) { KpiRow() },
        SampleCard(Pane.Start) { RevenueCard() },
        SampleCard(Pane.End) { SpendingCard(big = false) },
        SampleCard(Pane.End) { StepsCard() },
    )
    Scene.Line -> listOf(
        SampleCard(Pane.Start) { RevenueCompareCard() },
        SampleCard(Pane.End) { CashFlowCard() },
        SampleCard(Pane.End) { WatchlistCard() },
    )
    Scene.Bars -> listOf(
        SampleCard(Pane.Start) { StepsCard() },
        SampleCard(Pane.Start) { ActivityCard() },
        SampleCard(Pane.End) { MonthlySpendingCard() },
    )
    Scene.Donut -> listOf(
        SampleCard(Pane.Start) { SpendingCard(big = true) },
        SampleCard(Pane.End) { MacrosCard() },
        SampleCard(Pane.End) { GoalsCard() },
    )
}
