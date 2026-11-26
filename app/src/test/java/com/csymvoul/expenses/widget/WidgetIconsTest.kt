package com.csymvoul.expenses.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The widget uses the app's painted emblems and glyphs, never emoji or plain symbols. */
class WidgetIconsTest {
    private val layout = File("src/main/res/layout/widget_ledger.xml").readText()
    private val code = File("src/main/java/com/csymvoul/expenses/widget/LedgerWidget.kt").readText() +
        File("src/main/java/com/csymvoul/expenses/widget/WidgetContent.kt").readText()
    private val symbols = Regex("""\p{So}|✦|⚠|➕|android:text="\+"""")

    @Test fun `the heading, trip and Quick add use the emblems and the quill glyph`() {
        assertTrue(layout.contains("@drawable/emblem_logo_pouch"))
        assertTrue(layout.contains("@drawable/emblem_trip"))
        assertTrue(Regex("""android:id="@\+id/widget_add"\s+[^>]*android:src="@drawable/emblem_quill"""").containsMatchIn(layout))
        // Bronze on parchment by day, gold at night.
        assertTrue(File("src/main/res/drawable/emblem_quill.xml").readText().contains("@drawable/emblem_quill_bronze"))
        assertTrue(File("src/main/res/drawable-night/emblem_quill.xml").readText().contains("@drawable/emblem_quill_gold"))
        listOf("bronze", "gold").forEach { assertTrue(it, File("src/main/res/drawable-nodpi/emblem_quill_$it.png").exists()) }
    }

    @Test fun `every ward row has an emblem`() {
        (1..3).forEach { assertTrue(layout.contains("android:id=\"@+id/widget_ward${it}_icon\"")) }
        assertTrue(code.contains("setImageViewResource(icon, ward.icon)"))
    }

    @Test fun `no emoji or plain symbols are drawn on the widget`() {
        assertFalse(symbols.containsMatchIn(layout.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")))
        assertFalse(symbols.containsMatchIn(code.lines().filterNot { it.trimStart().startsWith("/") || it.trimStart().startsWith("*") }.joinToString("\n")))
    }
}
