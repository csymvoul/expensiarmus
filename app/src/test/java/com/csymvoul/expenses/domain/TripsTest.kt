package com.csymvoul.expenses.domain

import com.csymvoul.expenses.data.ExpenseStore
import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.model.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

class TripsTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun d(s: String) = LocalDate.parse(s)
    private val crete = Trip("crete", "Crete 2027", d("2027-07-10"), d("2027-07-16"), 120_000)
    private val rome = Trip("rome", "Rome", d("2026-11-20"), d("2026-11-28"))
    private var n = 0
    private fun e(date: String, cents: Long, cat: Category, pct: Int = 100, food: FoodType? = null, title: String = "t$n", trip: String? = null) =
        Expense("id${n++}", d(date), title, cents, cat, pct, food, trip)

    @Test fun `trips need a name, days in order and a sensible budget, and names are unique`() {
        assertEquals("Give it a name.", Trips.check(crete.copy(name = " ")).second)
        assertEquals("The last day is before the first.", Trips.check(crete.copy(end = d("2027-07-09"))).second)
        assertEquals("The budget must be an amount, or left empty.", Trips.check(crete.copy(budgetCents = -1)).second)
        assertNull(Trips.check(crete.copy(budgetCents = 0)).first!!.budgetCents)
        var list = Trips.save(emptyList(), crete).list!!
        list = Trips.save(list, rome).list!!
        assertEquals(listOf("crete", "rome"), list.map { it.id })
        assertEquals("You already have a trip with that name.", Trips.save(list, rome.copy(id = "other", name = "ROME")).error)
        assertEquals("Rome & Florence", Trips.save(list, rome.copy(name = "Rome & Florence")).list!!.first { it.id == "rome" }.name)
    }

    @Test fun `a trip is upcoming, on or done, and the ongoing one is ticked when recording`() {
        assertEquals(TripStatus(TripStatus.State.UPCOMING, 9, 0, 1), Trips.status(rome, d("2026-11-19")))
        assertEquals(TripStatus(TripStatus.State.ONGOING, 9, 3), Trips.status(rome, d("2026-11-22")))
        assertEquals(TripStatus(TripStatus.State.DONE, 9, 9), Trips.status(rome, d("2026-11-29")))
        assertEquals(listOf("rome", "crete"), Trips.toOffer(listOf(crete, rome), d("2026-11-24")).map { it.id })
        assertEquals(listOf("crete"), Trips.toOffer(listOf(crete, rome), d("2026-12-01")).map { it.id })
        assertEquals("rome", Trips.ongoing(listOf(crete, rome), d("2026-11-24"))!!.id)
        assertNull(Trips.ongoing(listOf(crete, rome), d("2026-12-01")))
    }

    @Test fun `trip expenses count in their category and month, and can be left out`() {
        val data = AppData(
            expenses = listOf(
                e("2026-11-21", 6_000, Category.ENTERTAINMENT, 50, FoodType.DINE_OUT, "Trattoria", "rome"),
                e("2026-11-05", 2_000, Category.ENTERTAINMENT, food = FoodType.DINE_OUT, title = "Souvlaki"),
                e("2026-11-22", 1_500, Category.FUEL, title = "Taxi", trip = "rome"),
            ),
            trips = listOf(rome),
        )
        assertEquals(5_000L, MonthlySummary.of(data, YearMonth.of(2026, 11)).categories.first { it.category == Category.ENTERTAINMENT }.spentCents)
        assertEquals(listOf("Souvlaki"), Trips.withoutTrips(data.expenses).map { it.title })
        val s = Trips.summary(data, "rome", d("2026-11-24"))!!
        assertEquals(4_500L, s.totalCents)
        assertEquals(2, s.expenses.size)
        assertNull(s.budget)
        assertEquals(900L, s.perDayCents)
        assertEquals(listOf("Dine out" to 3_000L, "Transport" to 1_500L), s.byCategory.map { it.label to it.cents })
        assertEquals(listOf(d("2026-11-22") to 1_500L, d("2026-11-21") to 3_000L), s.byDay)
        val budgeted = Trips.summary(data.copy(trips = listOf(rome.copy(budgetCents = 5_000))), "rome", d("2026-12-01"))!!
        assertEquals(90, budgeted.budget!!.percent)
        assertEquals(500L, budgeted.perDayCents)
        val after = Trips.delete(data, "rome")
        assertEquals(emptyList<Trip>(), after.trips)
        assertEquals(0, after.expenses.count { it.tripId != null })
        assertEquals(3, after.expenses.size)
    }

    @Test fun `trips are saved and loaded, and ties to a missing trip are dropped`() {
        val store = ExpenseStore(File(tmp.root, "expenses.json"))
        store.save(AppData(expenses = listOf(e("2026-11-21", 100, Category.OTHER, trip = "rome"), e("2026-11-22", 100, Category.OTHER, trip = "gone")), trips = listOf(rome, Trip("bad", " ", rome.start, rome.end))))
        val back = store.load()
        assertEquals(listOf(rome), back.trips)
        assertEquals(listOf("rome", null), back.expenses.map { it.tripId })
    }
}
