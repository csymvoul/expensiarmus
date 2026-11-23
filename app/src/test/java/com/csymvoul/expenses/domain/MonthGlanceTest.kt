package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthGlanceTest {
    private var n = 0
    private fun e(date: String, cents: Long, percent: Int = 100, cat: Category = Category.OTHER) =
        Expense(id = "id${n++}", date = LocalDate.parse(date), title = "t$n", amountCents = cents, category = cat, myPercent = percent)

    @Test fun `the Ledger lists the latest 5 until Show all`() {
        val list = (0 until 8).map { e("2026-11-${20 - it}", 100) }
        assertEquals(5, ExpenseQueries.LATEST_COUNT)
        assertEquals((20 downTo 16).map { LocalDate.of(2026, 11, it) }, ExpenseQueries.ledgerPreview(list, showAll = false).map { it.date })
        assertEquals(8, ExpenseQueries.ledgerPreview(list, showAll = true).size)
        assertEquals(3, ExpenseQueries.ledgerPreview(list.take(3), showAll = false).size)
    }

    @Test fun `a running month averages over the days so far and compares fairly`() {
        val list = listOf(
            e("2026-11-01", 45000, cat = Category.RENT), e("2026-11-03", 1000), e("2026-11-03", 2000, percent = 50),
            e("2026-11-20", 9000, cat = Category.SHOPPING),
            e("2026-10-02", 30000, cat = Category.RENT), e("2026-10-05", 500), e("2026-10-25", 99999),
            e("2026-12-01", 7000),
        )
        val g = ExpenseQueries.monthGlance(list, YearMonth.of(2026, 11), LocalDate.of(2026, 11, 10))
        assertEquals(56000L, g.totalCents)
        assertEquals(4, g.count)
        assertEquals(10, g.daysCounted)
        assertEquals(5600L, g.dailyAverageCents)
        assertEquals(LocalDate.of(2026, 11, 1), g.busiestDay)
        assertEquals(45000L, g.busiestCents)
        assertEquals(10, g.upToDay)
        assertEquals(YearMonth.of(2026, 10), g.previousMonth)
        assertEquals(30500L, g.previousCents)
        assertEquals(Math.round((56000 - 30500) * 100.0 / 30500).toInt(), g.changePercent)
    }

    @Test fun `a finished month averages over all its days and compares with the whole month before`() {
        val g = ExpenseQueries.monthGlance(listOf(e("2026-10-02", 3100), e("2026-09-30", 6200)), YearMonth.of(2026, 10), LocalDate.of(2026, 11, 23))
        assertEquals(31, g.daysCounted)
        assertEquals(100L, g.dailyAverageCents)
        assertNull(g.upToDay)
        assertEquals(-50, g.changePercent)
    }

    @Test fun `empty months, ties and rounding match the web app`() {
        val empty = ExpenseQueries.monthGlance(emptyList(), YearMonth.of(2026, 11), LocalDate.of(2026, 11, 5))
        assertEquals(0L, empty.totalCents)
        assertEquals(0L, empty.dailyAverageCents)
        assertNull(empty.busiestDay)
        assertNull(empty.changePercent)
        assertNull(ExpenseQueries.monthGlance(listOf(e("2026-11-02", 500)), YearMonth.of(2026, 11), LocalDate.of(2026, 11, 5)).changePercent)
        val tie = ExpenseQueries.monthGlance(
            listOf(e("2026-02-09", 300), e("2026-02-04", 300), e("2026-02-28", 14)), YearMonth.of(2026, 2), LocalDate.of(2026, 3, 1),
        )
        assertEquals(LocalDate.of(2026, 2, 4), tie.busiestDay)
        assertEquals(22L, tie.dailyAverageCents)
        assertEquals(28, ExpenseQueries.monthGlance(emptyList(), YearMonth.of(2026, 3), LocalDate.of(2026, 3, 31)).upToDay)
    }

    @Test fun `Record expense and Add expense carry the same pen`() {
        assertEquals("✒️", Words.WIZARD.fab.substringBefore(' '))
        assertEquals("✒️ Add expense", Words.PLAIN.fab)
    }
}
