package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/** Expenses of one month, with my-share total. */
data class MonthGroup(val month: YearMonth, val totalCents: Long, val expenses: List<Expense>)

object ExpenseQueries {
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
