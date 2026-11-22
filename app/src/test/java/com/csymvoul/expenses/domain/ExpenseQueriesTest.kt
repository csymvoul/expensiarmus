package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ExpenseQueriesTest {
    private var n = 0
    private fun e(
        date: String,
        title: String,
        cents: Long,
        cat: Category,
        percent: Int = 100,
        food: FoodType? = null,
    ) = Expense("id${n++}", LocalDate.parse(date), title, cents, cat, percent, food)

    private val expenses = listOf(
        e("2026-11-03", "Lidl", 4_000, Category.SUPERMARKET),
        e("2026-11-03", "Freddo espresso", 250, Category.ENTERTAINMENT, food = FoodType.DAY_TO_DAY),
        e("2026-11-14", "Σουβλάκι με φίλους", 2_000, Category.ENTERTAINMENT, 50, FoodType.DINE_OUT),
        e("2026-11-20", "Running shoes", 9_000, Category.SHOPPING),
        e("2026-10-31", "Earrings", 6_000, Category.SHOPPING),
        e("2025-12-24", "Gift for mum", 3_000, Category.OTHER),
        e("2026-11-28", "Cinema", 900, Category.ENTERTAINMENT),
    )

    // --- calendar ---

    @Test fun `daily totals use my share and only the month`() {
        val totals = ExpenseQueries.dailyTotals(expenses, YearMonth.of(2026, 11))
        assertEquals(4_250L, totals[LocalDate.of(2026, 11, 3)])
        assertEquals(1_000L, totals[LocalDate.of(2026, 11, 14)])
        assertNull(totals[LocalDate.of(2026, 10, 31)])
        assertEquals(4, totals.size)
    }

    @Test fun `expenses on a day`() {
        val day = ExpenseQueries.onDay(expenses, LocalDate.of(2026, 11, 3))
        assertEquals(listOf("Freddo espresso", "Lidl"), day.map { it.title })
        assertTrue(ExpenseQueries.onDay(expenses, LocalDate.of(2026, 11, 4)).isEmpty())
    }

    @Test fun `november 2026 grid starts on sunday the 1st after six blanks`() {
        // 1 Nov 2026 is a Sunday.
        val cells = ExpenseQueries.calendarCells(YearMonth.of(2026, 11))
        assertEquals(42, cells.size) // the 30th is a Monday, so six weeks
        assertEquals(List(6) { null }, cells.take(6))
        assertEquals(LocalDate.of(2026, 11, 1), cells[6])
        assertEquals(LocalDate.of(2026, 11, 30), cells[35])
        assertEquals(List(6) { null }, cells.takeLast(6))
        assertEquals(30, cells.filterNotNull().size)
    }

    @Test fun `grid is always whole weeks`() {
        for (m in 1..12) {
            val month = YearMonth.of(2026, m)
            val cells = ExpenseQueries.calendarCells(month)
            assertEquals(0, cells.size % 7)
            assertEquals(month.lengthOfMonth(), cells.filterNotNull().size)
            assertEquals(month.atDay(1).dayOfWeek.value - 1, cells.indexOfFirst { it != null })
        }
    }

    @Test fun `february 2027 starting on monday fills exactly four weeks`() {
        assertEquals(28, ExpenseQueries.calendarCells(YearMonth.of(2027, 2)).size)
    }

    // --- category ---

    @Test fun `category groups by month newest first`() {
        val groups = ExpenseQueries.byCategory(expenses, Category.SHOPPING)
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10)), groups.map { it.month })
        assertEquals(listOf(9_000L, 6_000L), groups.map { it.totalCents })
    }

    @Test fun `entertainment can be narrowed to one food type`() {
        val all = ExpenseQueries.byCategory(expenses, Category.ENTERTAINMENT)
        assertEquals(3, all.sumOf { it.expenses.size })
        assertEquals(1_000L + 250 + 900, all.single().totalCents)
        val dineOut = ExpenseQueries.byCategory(expenses, Category.ENTERTAINMENT, FoodType.DINE_OUT)
        assertEquals(listOf("Σουβλάκι με φίλους"), dineOut.flatMap { it.expenses }.map { it.title })
    }

    @Test fun `empty category has no groups`() {
        assertTrue(ExpenseQueries.byCategory(expenses, Category.RENT).isEmpty())
    }

    // --- search ---

    private fun titles(query: String, cats: Set<Category> = emptySet()) =
        ExpenseQueries.search(expenses, query, cats).map { it.title }

    @Test fun `search by part of title ignoring case`() =
        assertEquals(listOf("Running shoes"), titles("SHOE"))

    @Test fun `greek search ignores accents and final sigma`() {
        assertEquals(listOf("Σουβλάκι με φίλους"), titles("σουβλακι"))
        assertEquals(listOf("Σουβλάκι με φίλους"), titles("ΦΙΛΟΥΣ"))
    }

    @Test fun `search by category name`() =
        assertEquals(listOf("Running shoes", "Earrings"), titles("shopping"))

    @Test fun `search by food type`() =
        assertEquals(listOf("Σουβλάκι με φίλους"), titles("dine"))

    @Test fun `every word must match`() {
        assertEquals(listOf("Freddo espresso"), titles("espresso day-to-day"))
        assertTrue(titles("espresso shoes").isEmpty())
    }

    @Test fun `category filter alone lists the category, newest first`() =
        assertEquals(listOf("Cinema", "Σουβλάκι με φίλους", "Freddo espresso"), titles("", setOf(Category.ENTERTAINMENT)))

    @Test fun `category filter combines with text`() {
        assertEquals(listOf("Gift for mum"), titles("gift", setOf(Category.OTHER, Category.SHOPPING)))
        assertTrue(titles("gift", setOf(Category.SHOPPING)).isEmpty())
    }

    @Test fun `no query and no categories finds nothing`() {
        assertTrue(titles("").isEmpty())
        assertTrue(titles("   ").isEmpty())
    }

    @Test fun `normalize`() {
        assertEquals("cafe", ExpenseQueries.normalize(" Café "))
        assertEquals("σουπερ μαρκετ", ExpenseQueries.normalize("Σούπερ Μάρκετ"))
        assertEquals("λογαριασμοσ", ExpenseQueries.normalize("λογαριασμός"))
    }
}
