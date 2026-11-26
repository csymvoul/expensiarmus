package com.csymvoul.expenses.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SettingsOrderTest {
    @Test fun `Settings cards come in the chosen order`() {
        val src = File("src/main/java/com/csymvoul/expenses/ui/screens/SettingsScreen.kt").readText()
        val body = src.substring(src.indexOf("fun SettingsScreen("), src.indexOf("private fun UpdatesCard("))
        val at = listOf(
            "Text(\"Favourites\"", "Text(w.trips,", "emblem_set_reminder", "Text(\"Wizard words\"", "Text(\"Number format\"",
            "Text(\"Theme\"", "UpdatesCard(settings", "emblem_set_geminio", "emblem_set_apparate",
        ).map { body.indexOf(it) }
        assertTrue("every card is there", at.all { it > 0 })
        assertEquals(at.sorted(), at)
    }
}
