package io.github.halilozel1903.minicharts.sample

import io.github.halilozel1903.minicharts.core.ValueFormatter

/** Fixed sample data, so every launch and every screenshot looks the same. */
object SampleData {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    val revenue = listOf(3_200, 4_100, 3_900, 5_200, 6_100, 7_300, 9_800, 8_900, 7_700, 8_100, 9_100, 9_400)
    val revenueLastYear = listOf(2_800, 3_000, 3_600, 3_900, 4_400, 5_100, 6_000, 6_400, 6_100, 6_800, 7_200, 7_900)
    val cashFlow = listOf(1_200, -400, 800, 1_600, -900, 2_100, 2_800, 1_100, -300, 1_900, 2_400, 3_000)

    val balance = listOf(
        21.2, 21.4, 21.1, 21.6, 21.9, 21.7, 22.0, 22.4, 22.3, 22.1, 22.6, 22.9, 22.7, 23.1, 23.0,
        23.4, 23.2, 23.6, 23.9, 23.7, 24.0, 23.8, 24.2, 24.1, 24.4, 24.3, 24.6, 24.5, 24.7, 24.83,
    ).map { it * 1_000 }
    val income = listOf(6.1, 6.4, 7.0, 6.8, 7.4, 7.9, 8.4).map { it * 1_000 }
    val spending = listOf(3.9, 3.4, 3.8, 3.2, 3.5, 3.0, 3.1).map { it * 1_000 }
    val stepsTrend = listOf(6.8, 7.4, 6.9, 8.2, 7.6, 8.8, 9.1).map { it * 1_000 }

    val steps = listOf(6_200, 8_400, 7_100, 9_300, 5_400, 11_800, 4_200)
    val run = listOf(20, 0, 35, 25, 0, 50, 30)
    val walk = listOf(30, 45, 20, 35, 40, 25, 60)
    val cycle = listOf(0, 30, 0, 20, 45, 0, 15)

    val halfYear = months.take(6)
    val bills = listOf(900, 900, 950, 950, 980, 1_000)
    val food = listOf(420, 380, 450, 400, 520, 470)
    val travel = listOf(120, 0, 640, 200, 90, 820)

    val spendingLabels = listOf("Rent", "Groceries", "Transport", "Dining", "Other")
    val spendingValues = listOf(1_200, 520, 260, 310, 180)

    val macroLabels = listOf("Carbs", "Protein", "Fat")
    val macroValues = listOf(220, 140, 70)

    val dollars: ValueFormatter = ValueFormatter.affixed(prefix = "$")
    val grams: ValueFormatter = ValueFormatter.affixed(suffix = " g", base = ValueFormatter.Integer)
    val minutes: ValueFormatter = ValueFormatter.affixed(suffix = " min", base = ValueFormatter.Integer)
}
