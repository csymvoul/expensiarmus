package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

class MonthListTest {
    @Test fun `a month's full list groups by day, newest first, and can leave trips out`() {
        val list = listOf(
            Expense("a", LocalDate.parse("2026-11-21"), "Trattoria", 6_000, Category.ENTERTAINMENT, 50, FoodType.DINE_OUT, "rome"),
            Expense("b", LocalDate.parse("2026-11-21"), "Bus", 1_500, Category.FUEL),
            Expense("c", LocalDate.parse("2026-11-05"), "Gift", 2_000, Category.OTHER),
        )
        assertEquals(
            listOf(Triple(LocalDate.parse("2026-11-21"), 4_500L, listOf("Bus", "Trattoria")), Triple(LocalDate.parse("2026-11-05"), 2_000L, listOf("Gift"))),
            ExpenseQueries.byDay(list).map { Triple(it.date, it.totalCents, it.expenses.map { e -> e.title }) },
        )
        assertEquals(listOf(1_500L, 2_000L), ExpenseQueries.byDay(Trips.withoutTrips(list)).map { it.totalCents })
    }

    @Test fun `the Ledger opens the full list, and the expense page shows painted icons`() {
        val month = File("src/main/java/com/csymvoul/expenses/ui/screens/MonthScreen.kt").readText()
        assertTrue(month.contains("onOpenMonthList(summary.month)"))
        val detail = File("src/main/java/com/csymvoul/expenses/ui/screens/ExpenseDetailScreen.kt").readText()
        assertFalse(detail.contains(".emoji"))
        assertTrue(detail.contains("expense.category.icon)") && detail.contains("it.icon)") && detail.contains("R.drawable.emblem_trip)"))
    }
}
