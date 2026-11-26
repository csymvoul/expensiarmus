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

    @Test fun `the full list sums the month by category, leaving the wards out when trips are left out`() {
        val goals = com.csymvoul.expenses.model.Goals().set(Category.OTHER, java.time.YearMonth.of(2026, 11), 5_000, onwards = true)
        val data = com.csymvoul.expenses.model.AppData(
            expenses = listOf(
                Expense("a", LocalDate.parse("2026-11-21"), "Trattoria", 6_000, Category.ENTERTAINMENT, 100, FoodType.DINE_OUT, "rome"),
                Expense("c", LocalDate.parse("2026-11-05"), "Gift", 2_000, Category.OTHER),
            ),
            goals = goals,
        )
        val nov = java.time.YearMonth.of(2026, 11)
        val all = MonthlySummary.forList(data, nov, leaveTripsOut = false)
        assertEquals(8_000L, all.totalCents)
        assertEquals(6_000L, all.categories.first { it.category == Category.ENTERTAINMENT }.spentCents)
        assertEquals(5_000L, all.categories.first { it.category == Category.OTHER }.goal?.goalCents)
        val without = MonthlySummary.forList(data, nov, leaveTripsOut = true)
        assertEquals(2_000L, without.totalCents)
        assertEquals(0L, without.categories.first { it.category == Category.ENTERTAINMENT }.spentCents)
        assertEquals(null, without.categories.first { it.category == Category.OTHER }.goal)
        assertEquals(null, without.totalGoal)
    }

    @Test fun `the full list shows the category card and days that open when tapped, kept across an expense visit`() {
        val screen = File("src/main/java/com/csymvoul/expenses/ui/screens/MonthListScreen.kt").readText()
        assertTrue(screen.contains("MonthlySummary.forList(data, month, leaveOut)"))
        assertTrue(screen.contains("BreakdownCard(summary.categories, summary.totalCents"))
        // A day's expenses are listed only while it is open.
        assertTrue(Regex("""if \(open\) \{\s+items\(d\.expenses""").containsMatchIn(screen))
        val main = File("src/main/java/com/csymvoul/expenses/MainActivity.kt").readText()
        assertTrue(main.contains("onToggleDay = { d -> listOpenDays = if (d in listOpenDays) listOpenDays - d else listOpenDays + d }"))
        assertTrue(main.contains("onOpenMonthList = { listOpenDays = emptySet(); push(ALL + it) }"))
        assertTrue(main.contains("onCategory = { c, t -> push(categoryRoute(c, t, listMonth.toString())) }"))
    }
}
