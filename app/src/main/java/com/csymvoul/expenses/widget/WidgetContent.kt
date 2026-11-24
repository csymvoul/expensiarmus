package com.csymvoul.expenses.widget

import com.csymvoul.expenses.domain.GoalProgress
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.domain.MonthlySummary
import com.csymvoul.expenses.domain.Words
import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.ui.theme.emoji
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One goal bar on the widget. [percent] is clamped to 0..100 for the bar. */
data class WardLine(val text: String, val percent: Int, val isOver: Boolean)

/** What the home-screen widget shows, worked out apart from Android so it can be tested. */
data class WidgetContent(val heading: String, val total: String, val label: String, val wards: List<WardLine>, val emptyNote: String?) {
    companion object {
        private val MONTH = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

        /** This month's total and up to three goals, the closest to (or furthest over) their goal first. */
        fun of(data: AppData, month: YearMonth, words: Words): WidgetContent {
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
            )
        }
    }
}
