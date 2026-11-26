package com.csymvoul.expenses.ui

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Guards the 1.13.0 slip where the Ledger and Annals opened category pages for each other's period. */
class CategoryPeriodWiringTest {
    private val src = File("src/main/java/com/csymvoul/expenses/MainActivity.kt").readText()
    private fun block(start: String): String {
        val i = src.indexOf(start)
        return src.substring(i, src.indexOf("bottomBar", i))
    }

    @Test fun `Annals opens categories for the year or all time`() {
        assertTrue(block("AnnalsScreen(").contains("categoryRoute(c, t, if (annalsView == AnnalsView.YEAR) annalsYear.toString() else \"\")"))
    }

    @Test fun `the Ledger opens categories for its month`() {
        assertTrue(block("MonthScreen(").contains("categoryRoute(c, t, month.toString())"))
    }
}
