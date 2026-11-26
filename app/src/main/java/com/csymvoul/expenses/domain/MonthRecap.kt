package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * A finished month in one line, for the owl on the 1st and the card on the Ledger: the total, the change
 * from the month before and the category most went to. Same rules and wording as the web app.
 */
data class MonthRecap(
    val month: YearMonth,
    val totalCents: Long,
    val previousMonth: YearMonth,
    val changePercent: Int?,
    val topCategory: Category,
    val topCents: Long,
) {
    /** "October: €1,240.00, 8% less than September, most went to Diagon Alley." [name] gives the category's name (nickname or label). */
    fun text(name: (Category) -> String): String {
        val before = previousMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        val change = when {
            changePercent == null -> ""
            changePercent > 0 -> ", $changePercent% more than $before"
            changePercent < 0 -> ", ${-changePercent}% less than $before"
            else -> ", the same as $before"
        }
        return "${monthName(month)}: ${Money.format(totalCents)}$change, most went to ${name(topCategory)}."
    }

    companion object {
        fun monthName(m: YearMonth): String = m.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

        /** Null for a month with nothing recorded. */
        fun of(data: AppData, month: YearMonth): MonthRecap? {
            val s = MonthlySummary.of(data, month)
            if (s.totalCents <= 0) return null
            val previous = month.minusMonths(1)
            val previousCents = MonthlySummary.of(data, previous).totalCents
            val top = s.categories.fold(s.categories.first()) { a, c -> if (c.spentCents > a.spentCents) c else a }
            return MonthRecap(
                month = month,
                totalCents = s.totalCents,
                previousMonth = previous,
                changePercent = if (previousCents > 0) Math.round((s.totalCents - previousCents) * 100.0 / previousCents).toInt() else null,
                topCategory = top.category,
                topCents = top.spentCents,
            )
        }

        /** The month the Ledger's card sums up on [today]: the one just finished. */
        fun monthFor(today: LocalDate): YearMonth = YearMonth.from(today).minusMonths(1)
    }
}
