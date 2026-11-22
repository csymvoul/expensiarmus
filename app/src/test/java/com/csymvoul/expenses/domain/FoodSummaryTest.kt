package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FoodSummaryTest {
    private var n = 0
    private fun e(cents: Long, food: FoodType?, percent: Int = 100, cat: Category = Category.ENTERTAINMENT, month: Int = 11) =
        Expense("id${n++}", LocalDate.of(2026, month, 10), "t", cents, cat, percent, food)

    private val data = AppData(
        listOf(
            e(3_000, FoodType.DELIVERY),
            e(2_000, FoodType.DELIVERY, percent = 50), // counts €10
            e(6_000, FoodType.DINE_OUT),
            e(250, FoodType.DAY_TO_DAY),
            e(250, FoodType.DAY_TO_DAY),
            e(1_200, null),                              // cinema
            e(5_000, null, cat = Category.SUPERMARKET),  // not entertainment
            e(9_900, FoodType.DINE_OUT, month = 10),     // other month
        ),
    )
    private val food = MonthlySummary.of(data, YearMonth.of(2026, 11)).food

    @Test fun `totals use my share and only this month`() {
        assertEquals(4_000 + 6_000 + 500L, food.totalCents)
        assertEquals(1_200L, food.otherEntertainmentCents)
    }

    @Test fun `per type totals, counts and shares`() {
        val byType = food.types.associateBy { it.type }
        assertEquals(FoodType.entries, food.types.map { it.type })
        assertEquals(4_000L, byType.getValue(FoodType.DELIVERY).spentCents)
        assertEquals(2, byType.getValue(FoodType.DELIVERY).count)
        assertEquals(6_000L, byType.getValue(FoodType.DINE_OUT).spentCents)
        assertEquals(500L, byType.getValue(FoodType.DAY_TO_DAY).spentCents)
        assertEquals(2, byType.getValue(FoodType.DAY_TO_DAY).count)
        assertEquals(1f, food.types.sumOf { it.shareOfFood.toDouble() }.toFloat(), 0.0001f)
    }

    @Test fun `food is still part of the entertainment category total`() {
        val ent = MonthlySummary.of(data, YearMonth.of(2026, 11)).categories.first { it.category == Category.ENTERTAINMENT }
        assertEquals(food.totalCents + food.otherEntertainmentCents, ent.spentCents)
    }

    @Test fun `no food gives zeros`() {
        val empty = FoodSummary.of(emptyList())
        assertEquals(0L, empty.totalCents)
        assertEquals(listOf(0f, 0f, 0f), empty.types.map { it.shareOfFood })
    }
}
