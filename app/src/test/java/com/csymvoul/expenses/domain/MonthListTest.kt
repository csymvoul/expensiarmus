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

    @Test fun `the full list has no chart (that is on the Ledger), and its days open when tapped, kept across an expense visit`() {
        val screen = File("src/main/java/com/csymvoul/expenses/ui/screens/MonthListScreen.kt").readText()
        assertFalse(screen.contains("BreakdownCard") || screen.contains("DonutChart"))
        // A day's expenses are listed only while it is open.
        assertTrue(Regex("""if \(open\) \{\s+items\(d\.expenses""").containsMatchIn(screen))
        val main = File("src/main/java/com/csymvoul/expenses/MainActivity.kt").readText()
        assertTrue(main.contains("onToggleDay = { d -> listOpenDays = if (d in listOpenDays) listOpenDays - d else listOpenDays + d }"))
        assertTrue(main.contains("onOpenMonthList = { listOpenDays = emptySet(); push(ALL + it) }"))
    }

    @Test fun `categoryTotals - per category, biggest first, empty ones left out, with Entertainment's food types`() {
        fun x(id: String, cents: Long, c: Category, food: FoodType? = null, pct: Int = 100, trip: String? = null) =
            Expense(id, LocalDate.parse("2026-11-20"), id, cents, c, pct, food, trip)
        val list = listOf(
            x("dinner", 6_000, Category.ENTERTAINMENT, FoodType.DINE_OUT, trip = "rome"), x("pizza", 1_800, Category.ENTERTAINMENT, FoodType.DELIVERY),
            x("cinema", 1_200, Category.ENTERTAINMENT), x("rent", 50_000, Category.RENT),
            x("lidl", 4_250, Category.SUPERMARKET), x("coat", 9_000, Category.SHOPPING, pct = 50),
        )
        val t = ExpenseQueries.categoryTotals(list)
        assertEquals(listOf(Category.RENT to 50_000L, Category.ENTERTAINMENT to 9_000L, Category.SHOPPING to 4_500L, Category.SUPERMARKET to 4_250L), t.map { it.category to it.cents })
        assertEquals(738, Math.round(t[0].share * 1000)) // €500 of €677.50
        assertEquals(listOf(FoodType.DELIVERY to 1_800L, FoodType.DINE_OUT to 6_000L), t[1].food)
        assertEquals(1_200L, t[1].otherCents)
        assertEquals(emptyList<Pair<FoodType, Long>>(), t[0].food)
        val without = ExpenseQueries.categoryTotals(Trips.withoutTrips(list))
        assertEquals(listOf(FoodType.DELIVERY to 1_800L), without.first { it.category == Category.ENTERTAINMENT }.food)
        assertEquals(emptyList<CategoryLine>(), ExpenseQueries.categoryTotals(emptyList()))
    }

    @Test fun `the full list has a folding By vault card that follows the trips switch and keeps it into the category`() {
        val screen = File("src/main/java/com/csymvoul/expenses/ui/screens/MonthListScreen.kt").readText()
        assertTrue(screen.contains("val lines = remember(shown) { ExpenseQueries.categoryTotals(shown) }"))
        assertTrue(screen.contains("ByVaultCard(lines, catsOpen, onToggleCats) { c, t -> onCategory(c, t, leaveOut) }"))
        assertTrue(screen.contains("if (!open) return@ParchmentCard"))
        val main = File("src/main/java/com/csymvoul/expenses/MainActivity.kt").readText()
        assertTrue(main.contains("onCategory = { c, t, without -> push(categoryRoute(c, t, listMonth.toString(), without)) }"))
        assertTrue(main.contains("var listCatsOpen by rememberSaveable { mutableStateOf(true) }"))
        assertTrue(main.contains("startWithTrips = !startWithout"))
        val category = File("src/main/java/com/csymvoul/expenses/ui/screens/CategoryScreen.kt").readText()
        assertTrue(category.contains("mutableStateOf(startWithTrips)"))
        assertEquals("By vault", Words.WIZARD.byVault)
        assertEquals("By category", Words.PLAIN.byVault)
        assertEquals("categories", Words.PLAIN.vaults)
    }

    @Test fun `the Ledger's category and food cards are tight - rows and the chart closer together`() {
        val month = File("src/main/java/com/csymvoul/expenses/ui/screens/MonthScreen.kt").readText()
        val breakdown = month.substring(month.indexOf("internal fun BreakdownCard"), month.indexOf("fun FoodCard"))
        val food = month.substring(month.indexOf("fun FoodCard"))
        assertTrue(breakdown.contains("ParchmentCard(spacing = Arrangement.spacedBy(4.dp))"))
        assertTrue(breakdown.contains(".padding(vertical = 2.dp, horizontal = 4.dp)"))
        assertTrue(food.contains("ParchmentCard(spacing = Arrangement.spacedBy(4.dp))"))
    }
}
