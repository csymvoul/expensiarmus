package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.Goals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class GoalAlertsTest {
    private val nov = YearMonth.of(2026, 11)
    private var n = 0
    private fun spend(cents: Long, cat: Category = Category.ENTERTAINMENT, percent: Int = 100, date: String = "2026-11-10") =
        Expense("id${n++}", LocalDate.parse(date), "t", cents, cat, percent)

    private val goals = Goals().set(Category.ENTERTAINMENT, nov, 6_000, onwards = false)
    private fun data(vararg e: Expense) = AppData(e.toList(), goals)

    @Test fun `nothing below 80 percent`() = assertTrue(GoalAlerts.due(data(spend(4_700)), nov).isEmpty())

    @Test fun `warning at exactly 80 percent`() {
        val alerts = GoalAlerts.due(data(spend(4_800)), nov)
        assertEquals(listOf(80), alerts.map { it.level })
        assertEquals("🦉 80% of your Entertainment ward is spent", alerts[0].title)
        assertEquals("€48.00 of €60.00 in November · €12.00 left. Time to slow down.", alerts[0].body)
    }

    @Test fun `my share is what counts`() {
        // €96 split 50-50 is €48 of mine: 80%.
        assertEquals(listOf(80), GoalAlerts.due(data(spend(9_600, percent = 50)), nov).map { it.level })
    }

    @Test fun `reaching and going over the goal`() {
        val exact = GoalAlerts.due(data(spend(6_000)), nov).single()
        assertEquals(100, exact.level)
        assertEquals("⚡ Your Entertainment ward is broken", exact.title)
        assertEquals("€60.00 of €60.00 in November · nothing left.", exact.body)
        assertEquals("€65.00 of €60.00 in November · €5.00 over.", GoalAlerts.due(data(spend(6_500)), nov).single().body)
    }

    @Test fun `each warning only once per month`() {
        val d = data(spend(5_000))
        val sent = GoalAlerts.markSent(d, GoalAlerts.due(d, nov))
        assertTrue(GoalAlerts.due(sent.copy(expenses = sent.expenses + spend(100)), nov).isEmpty())
        // Going over later still sends the 100% warning once.
        val over = sent.copy(expenses = sent.expenses + spend(2_000))
        assertEquals(listOf(100), GoalAlerts.due(over, nov).map { it.level })
        val after = GoalAlerts.markSent(over, GoalAlerts.due(over, nov))
        assertTrue(GoalAlerts.due(after, nov).isEmpty())
    }

    @Test fun `jumping straight past the goal sends only the 100 percent warning and covers 80 too`() {
        val d = data(spend(7_000))
        val alerts = GoalAlerts.due(d, nov)
        assertEquals(listOf(100), alerts.map { it.level })
        assertEquals(setOf("ENTERTAINMENT|2026-11|80", "ENTERTAINMENT|2026-11|100"), GoalAlerts.markSent(d, alerts).alertsSent)
    }

    @Test fun `categories without a goal never warn`() =
        assertTrue(GoalAlerts.due(data(spend(99_999, Category.RENT)), nov).isEmpty())

    @Test fun `a new month starts fresh`() {
        val goalsBoth = Goals().set(Category.ENTERTAINMENT, nov, 6_000, onwards = true)
        val d = AppData(listOf(spend(5_000), spend(5_000, date = "2026-12-03")), goalsBoth)
        val novSent = GoalAlerts.markSent(d, GoalAlerts.due(d, nov))
        assertEquals(listOf(80), GoalAlerts.due(novSent, YearMonth.of(2026, 12)).map { it.level })
    }
}

class GoalScopeTest {
    private val nov = YearMonth.of(2026, 11)
    private fun entry(month: String, onwards: Boolean) =
        com.csymvoul.expenses.model.GoalEntry(Category.ENTERTAINMENT, month, 6_000, onwards)

    @Test fun `scope wording`() {
        assertEquals("Only November 2026", GoalScope.text(entry("2026-11", false), nov))
        assertEquals("From November 2026 onwards", GoalScope.text(entry("2026-11", true), nov))
        assertEquals("Every month since September 2026", GoalScope.text(entry("2026-09", true), nov))
        assertEquals("Every month", GoalScope.text(entry(Goals.LEGACY_START, true), nov))
    }
}
