package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * The calendar's month at a glance. [busiestDay] is the day with the most spent (the earlier one on a tie).
 * [upToDay] is set while the month is still running: the month before is then counted only up to that day.
 */
data class MonthGlance(
    val totalCents: Long,
    val count: Int,
    val daysCounted: Int,
    val dailyAverageCents: Long,
    val busiestDay: LocalDate?,
    val busiestCents: Long,
    val previousMonth: YearMonth,
    val previousCents: Long,
    val upToDay: Int?,
    val changePercent: Int?,
)

/** Expenses of one month, with my-share total. */
data class MonthGroup(val month: YearMonth, val totalCents: Long, val expenses: List<Expense>)

object ExpenseQueries {
    /** How many of the month's expenses the Ledger lists before "Show all". */
    const val LATEST_COUNT = 5

    /** The Ledger's list: the newest few, or all of them once "Show all" is tapped. Expects newest first. */
    fun ledgerPreview(expenses: List<Expense>, showAll: Boolean, n: Int = LATEST_COUNT): List<Expense> =
        if (showAll) expenses else expenses.take(n)

    /**
     * Total, count, average per day (over the days so far while [month] is running), busiest day and the
     * change from the month before (counted up to the same day while [month] is running, so it is fair).
     */
    fun monthGlance(expenses: List<Expense>, month: YearMonth, today: LocalDate): MonthGlance {
        val mine = expenses.filter { YearMonth.from(it.date) == month }
        val total = mine.sumOf { it.myShareCents }
        val running = YearMonth.from(today) == month
        val daysCounted = if (running) today.dayOfMonth else month.lengthOfMonth()
        val busiest = dailyTotals(expenses, month).entries
            .sortedWith(compareByDescending<Map.Entry<LocalDate, Long>> { it.value }.thenBy { it.key })
            .firstOrNull()
        val previousMonth = month.minusMonths(1)
        val upToDay = if (running) minOf(daysCounted, previousMonth.lengthOfMonth()) else null
        val previous = expenses
            .filter { YearMonth.from(it.date) == previousMonth && (upToDay == null || it.date.dayOfMonth <= upToDay) }
            .sumOf { it.myShareCents }
        return MonthGlance(
            totalCents = total,
            count = mine.size,
            daysCounted = daysCounted,
            dailyAverageCents = (total * 2 + daysCounted) / (daysCounted * 2L), // half up
            busiestDay = busiest?.key,
            busiestCents = busiest?.value ?: 0,
            previousMonth = previousMonth,
            previousCents = previous,
            upToDay = upToDay,
            changePercent = if (previous > 0) Math.round((total - previous) * 100.0 / previous).toInt() else null,
        )
    }

    private val newestFirst = compareByDescending<Expense> { it.date }.thenBy { it.title.lowercase() }

    /** My share spent on each day of [month] that has expenses. */
    fun dailyTotals(expenses: List<Expense>, month: YearMonth): Map<LocalDate, Long> =
        expenses.filter { YearMonth.from(it.date) == month }
            .groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.myShareCents } }

    fun onDay(expenses: List<Expense>, day: LocalDate): List<Expense> =
        expenses.filter { it.date == day }.sortedWith(newestFirst)

    /**
     * Calendar cells for [month], whole weeks starting on [firstDay]; null cells pad the
     * days before the 1st and after the last day.
     */
    fun calendarCells(month: YearMonth, firstDay: DayOfWeek = DayOfWeek.MONDAY): List<LocalDate?> {
        val lead = (month.atDay(1).dayOfWeek.value - firstDay.value + 7) % 7
        val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
        val cells = List<LocalDate?>(lead) { null } + days
        return cells + List((7 - cells.size % 7) % 7) { null }
    }

    /**
     * All expenses of [category] grouped by month, newest month first. For Entertainment,
     * [foodType] narrows it to one kind of food.
     */
    /**
     * The expenses of a period: "" is all time, "2026" a year, "2026-11" a month. Category pages use it so
     * a category opened from a month, a year or all time lists just that span.
     */
    fun inPeriod(expenses: List<Expense>, period: String = ""): List<Expense> =
        if (period.isEmpty()) expenses else expenses.filter { it.date.toString().startsWith("$period-") }

    /** "for this month" / "in 2026" / "of all time": what a category opened from that card will list. */
    fun periodWords(period: String): String = when (period.length) {
        0 -> "of all time"
        7 -> "for this month"
        else -> "in $period"
    }

    /** The category page's heading for a period: "November 2026", "2026" or "All time". */
    fun periodTitle(period: String): String = when (period.length) {
        0 -> "All time"
        7 -> YearMonth.parse(period).format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
        else -> period
    }

    fun byCategory(expenses: List<Expense>, category: Category, foodType: FoodType? = null): List<MonthGroup> =
        expenses.filter { it.category == category && (foodType == null || it.foodType == foodType) }
            .groupBy { YearMonth.from(it.date) }
            .toSortedMap(compareByDescending { it })
            .map { (month, list) -> MonthGroup(month, list.sumOf { it.myShareCents }, list.sortedWith(newestFirst)) }

    /**
     * Expenses whose title, category or food type contain every word of [query], limited to
     * [categories] when that is not empty. Case and Greek accents are ignored. With no query
     * and no categories there is nothing to search for, so the result is empty.
     */
    fun search(expenses: List<Expense>, query: String, categories: Set<Category> = emptySet()): List<Expense> {
        val words = normalize(query).split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty() && categories.isEmpty()) return emptyList()
        return expenses.filter { e ->
            (categories.isEmpty() || e.category in categories) && words.all { w ->
                normalize(e.title).contains(w) ||
                    normalize(e.category.label).contains(w) ||
                    (e.foodType?.let { normalize(it.label).contains(w) } ?: false)
            }
        }.sortedWith(newestFirst)
    }

    /** Lower case without accents, so "Σουβλάκι" matches "σουβλακι" and "Café" matches "cafe". */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
            .replace('ς', 'σ')
            .trim()
}
