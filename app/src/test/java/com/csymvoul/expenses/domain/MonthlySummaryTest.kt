package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.Goals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthlySummaryTest {
    private val nov = YearMonth.of(2026, 11)
    private var n = 0
    private fun e(day: LocalDate, cents: Long, cat: Category, percent: Int = 100, title: String = "t${n}") =
        Expense("id${n++}", day, title, cents, cat, percent)

    private val data = AppData(
        expenses = listOf(
            e(LocalDate.of(2026, 11, 1), 60_000, Category.RENT),
            e(LocalDate.of(2026, 11, 3), 8_000, Category.SUPERMARKET),
            e(LocalDate.of(2026, 11, 20), 6_000, Category.ENTERTAINMENT, 50), // dinner split 50-50
            e(LocalDate.of(2026, 11, 30), 9_000, Category.BILLS, 50),
            e(LocalDate.of(2026, 10, 31), 99_999, Category.OTHER), // previous month
            e(LocalDate.of(2026, 12, 1), 99_999, Category.OTHER),  // next month
        ),
    )

    @Test fun `only expenses of the month count, using my share`() {
        val s = MonthlySummary.of(data, nov)
        assertEquals(4, s.expenses.size)
        assertEquals(60_000 + 8_000 + 3_000 + 4_500L, s.totalCents)
        assertEquals(60_000 + 8_000 + 6_000 + 9_000L, s.paidTotalCents)
    }

    @Test fun `expenses are newest first`() {
        val days = MonthlySummary.of(data, nov).expenses.map { it.date.dayOfMonth }
        assertEquals(listOf(30, 20, 3, 1), days)
    }

    @Test fun `category totals cover every category and add up`() {
        val s = MonthlySummary.of(data, nov)
        assertEquals(Category.entries, s.categories.map { it.category })
        val byCat = s.categories.associateBy { it.category }
        assertEquals(3_000L, byCat.getValue(Category.ENTERTAINMENT).spentCents)
        assertEquals(0L, byCat.getValue(Category.OTHER).spentCents)
        assertEquals(s.totalCents, s.categories.sumOf { it.spentCents })
        assertEquals(1f, s.categories.sumOf { it.shareOfMonth.toDouble() }.toFloat(), 0.0001f)
    }

    @Test fun `no goals means no progress`() {
        assertTrue(MonthlySummary.of(data, nov).categories.all { it.goal == null })
    }

    @Test fun `category goals for the month report progress`() {
        val goals = Goals().set(Category.ENTERTAINMENT, nov, 2_000, onwards = false).set(Category.RENT, nov, 100_000, onwards = true)
        val s = MonthlySummary.of(data.copy(goals = goals), nov)
        val fun_ = s.categories.first { it.category == Category.ENTERTAINMENT }.goal!!
        assertEquals(150, fun_.percent)
        assertTrue(fun_.isOver)
        assertEquals(-1_000L, fun_.remainingCents)
        assertEquals(60, s.categories.first { it.category == Category.RENT }.goal!!.percent)
        assertNull(s.categories.first { it.category == Category.BILLS }.goal)
    }

    @Test fun `a goal for one month does not show in another`() {
        val goals = Goals().set(Category.ENTERTAINMENT, nov, 2_000, onwards = false)
        assertTrue(MonthlySummary.of(data.copy(goals = goals), YearMonth.of(2026, 12)).categories.all { it.goal == null })
    }

    @Test fun `empty month`() {
        val s = MonthlySummary.of(data, YearMonth.of(2025, 1))
        assertEquals(0L, s.totalCents)
        assertTrue(s.expenses.isEmpty())
        assertTrue(s.categories.all { it.shareOfMonth == 0f })
    }

    @Test fun `goal description under and over`() {
        assertEquals("50% of €1,000.00 · €500.00 left", GoalProgress(100_000, 50_000).describe())
        assertEquals("100% of €10.00 · €0.00 left", GoalProgress(1_000, 1_000).describe())
        assertEquals(
            "150% of €20.00 · €10.00 over. The Protego ward is broken!",
            GoalProgress(2_000, 3_000).describe(),
        )
    }

    @Test fun `goal progress maths`() {
        assertEquals(0.5f, GoalProgress(1_000, 500).fraction, 0f)
        assertEquals(34, GoalProgress(300, 101).percent) // 33.67% rounds to 34
        assertEquals(0, GoalProgress(0, 500).percent)
        assertFalse(GoalProgress(500, 500).isOver)
    }
}
