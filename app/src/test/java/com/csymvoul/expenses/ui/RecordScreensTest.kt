package com.csymvoul.expenses.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RecordScreensTest {
    private fun src(name: String) = File("src/main/java/com/csymvoul/expenses/ui/screens/$name").readText()

    @Test fun `recording never suggests earlier titles`() {
        for (screen in listOf("QuickAddSheet.kt", "EditExpenseScreen.kt")) {
            val s = src(screen)
            assertFalse(screen, s.contains("SuggestionList") || s.contains("Suggestions.of("))
        }
    }

    @Test fun `Quick add is compact`() {
        val s = src("QuickAddSheet.kt")
        assertTrue(s.contains("Modifier.weight(1f).height(44.dp)"))
        assertTrue(s.contains("EmblemBadge(look.first, size = 34.dp)"))
        assertTrue(s.contains("verticalArrangement = Arrangement.spacedBy(7.dp)"))
        assertTrue(s.contains("MaterialTheme.typography.headlineLarge"))
    }
}
