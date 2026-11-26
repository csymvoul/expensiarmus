package com.csymvoul.expenses.ui

import com.csymvoul.expenses.R
import com.csymvoul.expenses.domain.Words
import com.csymvoul.expenses.ui.screens.AnnalsView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TripWiringTest {
    private fun src(path: String) = File("src/main/java/com/csymvoul/expenses/$path").readText()

    @Test fun `Annals has a Trips view and trips have their screens`() {
        assertEquals(listOf(AnnalsView.YEAR, AnnalsView.ALL_TIME, AnnalsView.TRIPS), AnnalsView.entries)
        val main = src("MainActivity.kt")
        assertTrue(main.contains("route == TRIPS -> TripsScreen("))
        assertTrue(main.contains("route.startsWith(TRIP) -> key(route) {"))
        assertTrue(main.contains("CompositionLocalProvider(LocalTripNames provides tripNames)"))
    }

    @Test fun `recording offers the trips and ties the expense`() {
        assertTrue(src("ui/screens/QuickAddSheet.kt").contains("TripChips(trips, tripId) { tripId = it }"))
        assertTrue(src("ui/screens/QuickAddSheet.kt").contains("tripId = tripId,"))
        assertTrue(src("ui/screens/EditExpenseScreen.kt").contains("TripChips(trips, tripId) { tripId = it }"))
        assertTrue(src("ui/screens/EditExpenseScreen.kt").contains("tripId = tripId,"))
        assertTrue(src("ui/screens/CategoryScreen.kt").contains("val groups = if (hasTrips && !withTrips) groupsWithoutTrips else groupsAll"))
    }

    @Test fun `trips have their painted badge and words`() {
        assertTrue(R.drawable.emblem_trip != 0)
        for (f in listOf("ui/screens/TripScreens.kt", "ui/screens/AnnalsScreen.kt", "ui/screens/SettingsScreen.kt")) {
            assertTrue(f, src(f).contains("R.drawable.emblem_trip"))
        }
        assertEquals("Portkeys", Words.WIZARD.trips)
        assertEquals("Trips", Words.PLAIN.trips)
    }
}
