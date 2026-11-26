package com.csymvoul.expenses.widget

import com.csymvoul.expenses.domain.GoalProgress
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.domain.MonthlySummary
import com.csymvoul.expenses.domain.Words
import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.ui.theme.emoji
import java.time.LocalDate
import java.time.YearMonth
import com.csymvoul.expenses.domain.Trips
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One goal bar on the widget. [percent] is clamped to 0..100 for the bar. */
data class WardLine(val text: String, val percent: Int, val isOver: Boolean)

/** What the home-screen widget shows, worked out apart from Android so it can be tested. */
data class WidgetContent(
    val heading: String,
    val total: String,
    val label: String,
    val wards: List<WardLine>,
    val emptyNote: String?,
    /** The trip on today: "✦ Rome · day 3 of 9 · €60.00 of €500.00", or null. */
    val trip: String? = null,
) {
    companion object {
        private val MONTH = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

        /** This month's total and up to three goals, the closest to (or furthest over) their goal first. */
        fun of(data: AppData, month: YearMonth, words: Words, today: LocalDate = LocalDate.now()): WidgetContent {
            val s = MonthlySummary.of(data, month)
            fun line(name: String, g: GoalProgress) = WardLine(
                text = "$name · ${Money.format(g.spentCents)} of ${Money.format(g.goalCents)}",
                percent = g.percent.coerceIn(0, 100),
                isOver = g.isOver,
            )
            // The whole-month ward first, then the category wards closest to (or furthest over) their goal.
            val whole = listOfNotNull(s.totalGoal?.let { line("✦ ${words.wholeMonth}", it) })
            val wards = whole + s.categories
                .filter { it.goal != null }
                .sortedByDescending { it.goal!!.fraction }
                .take(3 - whole.size)
                .map { c -> line("${c.category.emoji} ${c.category.label}", c.goal!!) }
            return WidgetContent(
                heading = "Expensiarmus · ${month.format(MONTH)}",
                total = Money.format(s.totalCents),
                label = words.spentThisMonth,
                wards = wards,
                emptyNote = if (wards.isEmpty()) words.noGoals else null,
                trip = Trips.ongoing(data.trips, today)?.let { Trips.summary(data, it.id, today) }?.let { s ->
                    "✦ ${s.trip.name} · ${Trips.statusText(s.status)} · ${Money.format(s.totalCents)}" +
                        (s.budget?.let { " of ${Money.format(it.goalCents)}" } ?: "")
                },
            )
        }
    }
}
