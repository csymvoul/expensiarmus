package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.Goals
import com.csymvoul.expenses.widget.WidgetContent
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class WholeMonthGoalTest {
    private val nov = YearMonth.of(2026, 11)
    private var n = 0
    private fun e(date: String, cents: Long, cat: Category = Category.OTHER) = Expense("id${n++}", LocalDate.parse(date), "t$n", cents, cat)

    @Test fun `a whole-month ward watches everything spent, next to the category wards`() {
        val goals = Goals().set(null, nov, 100_000, onwards = true).set(Category.ENTERTAINMENT, nov, 20_000, onwards = false)
        assertEquals(100_000L, goals.goalFor(null, nov)!!.cents)
        assertEquals(100_000L, goals.goalFor(null, YearMonth.of(2027, 2))!!.cents)
        assertNull(goals.goalFor(null, YearMonth.of(2026, 10)))
        assertEquals(20_000L, goals.goalFor(Category.ENTERTAINMENT, nov)!!.cents)
        assertNull(goals.goalFor(Category.RENT, nov))
        val data = AppData(listOf(e("2026-11-01", 45_000, Category.RENT), e("2026-11-05", 40_000, Category.ENTERTAINMENT)), goals)
        val s = MonthlySummary.of(data, nov)
        assertEquals(GoalProgress(100_000, 85_000), s.totalGoal)
        assertEquals(85, s.totalGoal!!.percent)
        assertNull(MonthlySummary.of(data.copy(goals = Goals()), nov).totalGoal)
        val off = goals.remove(null, nov, onwards = false)
        assertNull(off.goalFor(null, nov))
        assertEquals(100_000L, off.goalFor(null, YearMonth.of(2026, 12))!!.cents)
    }

    @Test fun `it warns at 80 percent and when broken, once each`() {
        val goals = Goals().set(null, nov, 10_000, onwards = false)
        fun at(cents: Long, sent: Set<String> = emptySet()) = GoalAlerts.due(AppData(listOf(e("2026-11-02", cents)), goals, sent), nov)
        assertEquals(emptyList<GoalAlert>(), at(7_000))
        val eighty = at(8_500).single()
        assertNull(eighty.category)
        assertEquals(Words.WIZARD.alert80("monthly"), eighty.title)
        assertTrue(eighty.body.startsWith("€85.00 of €100.00 in November"))
        assertEquals(emptyList<GoalAlert>(), at(8_500, setOf("TOTAL|2026-11|80")))
        val broken = at(12_000, setOf("TOTAL|2026-11|80")).single()
        assertEquals(100, broken.level)
        assertEquals(setOf("TOTAL|2026-11|80", "TOTAL|2026-11|100"), GoalAlerts.markSent(AppData(), listOf(broken)).alertsSent)
    }

    @Test fun `saved data with and without whole-month wards loads`() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val goals = Goals().set(null, nov, 50_000, onwards = true).set(Category.FUEL, nov, 6_000, onwards = false)
        val back = json.decodeFromString(Goals.serializer(), json.encodeToString(Goals.serializer(), goals))
        assertEquals(goals, back)
        val old = json.decodeFromString(Goals.serializer(), """{"entries":[{"category":"RENT","month":"2026-11","cents":50000,"onwards":true}]}""")
        assertEquals(Category.RENT, old.entries.single().category)
    }

    @Test fun `ward keys tell the whole month from the categories`() {
        assertEquals("TOTAL", GoalScope.key(null))
        assertEquals("RENT", GoalScope.key(Category.RENT))
        assertNull(GoalScope.target("TOTAL"))
        assertEquals(Category.FUEL, GoalScope.target("FUEL"))
        assertEquals(listOf<Category?>(null) + Category.entries, GoalScope.targets)
        assertEquals("Back to November 2026", Words.WIZARD.thisMonth("November 2026"))
    }

    @Test fun `the widget shows the whole-month ward first`() {
        val goals = Goals().set(null, nov, 100_000, false).set(Category.RENT, nov, 50_000, false)
            .set(Category.OTHER, nov, 1_000, false).set(Category.FUEL, nov, 1_000, false)
        val data = AppData(listOf(e("2026-11-01", 45_000, Category.RENT), e("2026-11-02", 900), e("2026-11-03", 500, Category.FUEL)), goals)
        val w = WidgetContent.of(data, nov, Words.WIZARD)
        assertEquals(3, w.wards.size)
        assertEquals("✦ Whole month · €464.00 of €1,000.00", w.wards[0].text)
        assertTrue(w.wards.drop(1).none { it.text.startsWith("✦") })
    }
}
