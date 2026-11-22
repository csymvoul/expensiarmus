package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

/** One bar of the Annals chart: a month of a year, or a whole year. */
data class PeriodBar(val label: String, val cents: Long, val month: YearMonth? = null, val year: Int? = null)

/** Totals over a longer period (a year, or everything). All amounts are my share. */
data class PeriodSummary(
    val totalCents: Long,
    val paidTotalCents: Long,
    val expenseCount: Int,
    /** Months the average is spread over (see [year] and [allTime]). */
    val monthsCounted: Int,
    val bars: List<PeriodBar>,
    val categories: List<CategoryTotal>,
    val food: FoodSummary,
    val biggest: Expense?,
    /** The same months one year earlier; null when there is nothing to compare with. */
    val previousTotalCents: Long?,
) {
    val averagePerMonthCents: Long
        get() = if (monthsCounted > 0) (totalCents + monthsCounted / 2) / monthsCounted else 0

    /** Change against [previousTotalCents] in whole percent; null without a previous total. */
    val changePercent: Int?
        get() = previousTotalCents?.takeIf { it > 0 }?.let { ((totalCents - it) * 100.0 / it).roundToInt() }

    companion object {
        /**
         * One calendar year. The average counts the months from January (or the first month
         * anything was recorded) up to the current month (or December for past years). For the
         * current year the comparison uses the same months of the previous year.
         */
        fun year(data: AppData, year: Int, today: LocalDate): PeriodSummary {
            val inYear = data.expenses.filter { it.date.year == year }
            val first = data.expenses.minOfOrNull { YearMonth.from(it.date) }
            val lastInYear = inYear.maxOfOrNull { YearMonth.from(it.date) }
            val start = listOfNotNull(YearMonth.of(year, 1), first).max()
            val end = listOfNotNull(minOf(YearMonth.of(year, 12), YearMonth.from(today)), lastInYear).max()
            val throughMonth = if (year == today.year) today.monthValue else 12
            val previous = data.expenses
                .filter { it.date.year == year - 1 && it.date.monthValue <= throughMonth }
                .sumOf { it.myShareCents }
            return build(
                expenses = inYear,
                monthsCounted = if (first == null) 0 else monthsBetween(start, end),
                bars = (1..12).map { m ->
                    val ym = YearMonth.of(year, m)
                    PeriodBar(
                        label = ym.month.getDisplayName(TextStyle.NARROW, Locale.ENGLISH),
                        cents = inYear.filter { it.date.monthValue == m }.sumOf { it.myShareCents },
                        month = ym,
                    )
                },
                previousTotalCents = previous.takeIf { data.expenses.any { it.date.year == year - 1 } },
            )
        }

        /** Everything recorded, with one bar per year from the first year to this year. */
        fun allTime(data: AppData, today: LocalDate): PeriodSummary {
            val first = data.expenses.minOfOrNull { YearMonth.from(it.date) }
            val last = data.expenses.maxOfOrNull { YearMonth.from(it.date) }
            val years = if (first == null) emptyList() else (first.year..maxOf(today.year, last!!.year)).toList()
            return build(
                expenses = data.expenses,
                monthsCounted = if (first == null) 0 else monthsBetween(first, maxOf(YearMonth.from(today), last!!)),
                bars = years.map { y ->
                    PeriodBar(
                        label = if (years.size > 5) "'" + (y % 100).toString().padStart(2, '0') else y.toString(),
                        cents = data.expenses.filter { it.date.year == y }.sumOf { it.myShareCents },
                        year = y,
                    )
                },
                previousTotalCents = null,
            )
        }

        private fun monthsBetween(start: YearMonth, end: YearMonth): Int =
            if (end < start) 0 else ChronoUnit.MONTHS.between(start, end).toInt() + 1

        private fun build(expenses: List<Expense>, monthsCounted: Int, bars: List<PeriodBar>, previousTotalCents: Long?): PeriodSummary {
            val total = expenses.sumOf { it.myShareCents }
            return PeriodSummary(
                totalCents = total,
                paidTotalCents = expenses.sumOf { it.amountCents },
                expenseCount = expenses.size,
                monthsCounted = monthsCounted,
                bars = bars,
                categories = Category.entries.map { c ->
                    val spent = expenses.filter { it.category == c }.sumOf { it.myShareCents }
                    CategoryTotal(c, spent, if (total > 0) spent.toFloat() / total else 0f, goal = null)
                },
                food = FoodSummary.of(expenses),
                biggest = expenses.maxWithOrNull(compareBy<Expense> { it.myShareCents }.thenBy { it.date }),
                previousTotalCents = previousTotalCents,
            )
        }
    }
}
