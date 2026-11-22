package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class PeriodSummaryTest {
    private val today = LocalDate.of(2026, 11, 22)
    private var n = 0
    private fun e(date: String, cents: Long, cat: Category = Category.OTHER, percent: Int = 100, food: FoodType? = null) =
        Expense("id${n++}", LocalDate.parse(date), "t$n", cents, cat, percent, food)

    private val data = AppData(
        listOf(
            e("2025-03-10", 10_000, Category.RENT),
            e("2025-12-01", 50_000, Category.RENT),
            e("2026-01-05", 60_000, Category.RENT),
            e("2026-01-20", 4_000, Category.FUEL),
            e("2026-03-02", 6_000, Category.FUEL, 50),
            e("2026-11-21", 2_000, Category.ENTERTAINMENT, food = FoodType.DINE_OUT),
        ),
    )

    @Test fun `year totals use my share and only that year`() {
        val y = PeriodSummary.year(data, 2026, today)
        assertEquals(60_000 + 4_000 + 3_000 + 2_000L, y.totalCents)
        assertEquals(60_000 + 4_000 + 6_000 + 2_000L, y.paidTotalCents)
        assertEquals(4, y.expenseCount)
        assertEquals(2_000L, y.food.totalCents)
    }

    @Test fun `year has twelve monthly bars`() {
        val bars = PeriodSummary.year(data, 2026, today).bars
        assertEquals(12, bars.size)
        assertEquals(listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"), bars.map { it.label })
        assertEquals(64_000L, bars[0].cents)
        assertEquals(3_000L, bars[2].cents)
        assertEquals(0L, bars[1].cents)
        assertEquals(YearMonth.of(2026, 11), bars[10].month)
        assertEquals(bars.sumOf { it.cents }, PeriodSummary.year(data, 2026, today).totalCents)
    }

    @Test fun `current year averages over months so far`() {
        val y = PeriodSummary.year(data, 2026, today)
        assertEquals(11, y.monthsCounted)
        assertEquals((69_000L + 5) / 11, y.averagePerMonthCents)
    }

    @Test fun `first year counts from the first recorded month`() {
        // First expense is March 2025, so 2025 counts March to December.
        assertEquals(10, PeriodSummary.year(data, 2025, today).monthsCounted)
    }

    @Test fun `future and empty years count nothing`() {
        val future = PeriodSummary.year(data, 2027, today)
        assertEquals(0L, future.totalCents)
        assertEquals(0, future.monthsCounted)
        assertEquals(0L, future.averagePerMonthCents)
        assertEquals(0, PeriodSummary.year(AppData(), 2026, today).monthsCounted)
    }

    @Test fun `current year compares with the same months of last year`() {
        val y = PeriodSummary.year(data, 2026, today)
        // Jan to Nov 2025: only the March expense counts, December is left out.
        assertEquals(10_000L, y.previousTotalCents)
        assertEquals(590, y.changePercent)
    }

    @Test fun `past year compares with the whole previous year`() {
        val withOld = data.copy(expenses = data.expenses + e("2024-12-31", 30_000))
        val y = PeriodSummary.year(withOld, 2025, today)
        assertEquals(30_000L, y.previousTotalCents)
        assertEquals(100, y.changePercent)
    }

    @Test fun `no previous year means no comparison`() {
        val y = PeriodSummary.year(data, 2025, today)
        assertNull(y.previousTotalCents)
        assertNull(y.changePercent)
    }

    @Test fun `biggest expense by my share`() {
        assertEquals(60_000L, PeriodSummary.year(data, 2026, today).biggest!!.amountCents)
        assertNull(PeriodSummary.year(data, 2027, today).biggest)
    }

    @Test fun `category totals cover every category including fuel`() {
        val cats = PeriodSummary.year(data, 2026, today).categories
        assertEquals(Category.entries, cats.map { it.category })
        assertEquals(7_000L, cats.first { it.category == Category.FUEL }.spentCents)
        assertTrue(cats.all { it.goal == null })
        assertEquals(1f, cats.sumOf { it.shareOfMonth.toDouble() }.toFloat(), 0.0001f)
    }

    @Test fun `all time has one bar per year up to this year`() {
        val all = PeriodSummary.allTime(data, today)
        assertEquals(listOf("2025", "2026"), all.bars.map { it.label })
        assertEquals(listOf(60_000L, 69_000L), all.bars.map { it.cents })
        assertEquals(129_000L, all.totalCents)
        // March 2025 to November 2026.
        assertEquals(21, all.monthsCounted)
        assertNull(all.changePercent)
    }

    @Test fun `many years use short labels`() {
        val old = data.copy(expenses = data.expenses + e("2019-06-01", 100))
        assertEquals("'19", PeriodSummary.allTime(old, today).bars.first().label)
        assertEquals(8, PeriodSummary.allTime(old, today).bars.size)
    }

    @Test fun `all time with no data is empty`() {
        val all = PeriodSummary.allTime(AppData(), today)
        assertTrue(all.bars.isEmpty())
        assertEquals(0, all.monthsCounted)
        assertEquals(0L, all.totalCents)
    }
}
