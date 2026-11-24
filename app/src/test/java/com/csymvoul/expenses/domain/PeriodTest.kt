package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PeriodTest {
    private var n = 0
    private fun e(date: String, cents: Long, cat: Category = Category.OTHER) =
        Expense("id${n++}", LocalDate.parse(date), "t$n", cents, cat)

    private val list = listOf(
        e("2026-11-03", 100), e("2026-11-28", 200), e("2026-10-30", 400),
        e("2025-11-03", 800), e("2026-01-01", 1600, Category.RENT),
    )

    @Test fun `a category page lists the month, the year or all time it was opened for`() {
        assertEquals(5, ExpenseQueries.inPeriod(list, "").size)
        assertEquals(listOf(100L, 200L), ExpenseQueries.inPeriod(list, "2026-11").map { it.amountCents })
        assertEquals(listOf(100L, 200L, 400L, 1600L), ExpenseQueries.inPeriod(list, "2026").map { it.amountCents })
        assertEquals(listOf(800L), ExpenseQueries.inPeriod(list, "2025").map { it.amountCents })
        val month = ExpenseQueries.byCategory(ExpenseQueries.inPeriod(list, "2026-11"), Category.OTHER)
        assertEquals(listOf(YearMonth.of(2026, 11) to 300L), month.map { it.month to it.totalCents })
        val year = ExpenseQueries.byCategory(ExpenseQueries.inPeriod(list, "2026"), Category.OTHER)
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10)), year.map { it.month })
    }

    @Test fun `the page says which span it shows`() {
        assertEquals("November 2026", ExpenseQueries.periodTitle("2026-11"))
        assertEquals("2026", ExpenseQueries.periodTitle("2026"))
        assertEquals("All time", ExpenseQueries.periodTitle(""))
        assertEquals("for this month", ExpenseQueries.periodWords("2026-11"))
        assertEquals("in 2026", ExpenseQueries.periodWords("2026"))
        assertEquals("of all time", ExpenseQueries.periodWords(""))
    }
}
