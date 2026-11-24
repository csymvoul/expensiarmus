package com.csymvoul.expenses.domain

import com.csymvoul.expenses.data.Settings
import com.csymvoul.expenses.data.ThemeMode
import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.model.Goals
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class GreekMoneyTest {
    @After fun reset() { Money.style = MoneyStyle.ENGLISH }

    @Test fun `greek style`() {
        assertEquals("1.234,56 €", Money.format(123_456, MoneyStyle.GREEK))
        assertEquals("-3,10 €", Money.format(-310, MoneyStyle.GREEK))
        assertEquals("0,05 €", Money.format(5, MoneyStyle.GREEK))
        assertEquals("45€", Money.compact(4_520, MoneyStyle.GREEK))
        assertEquals("1,2k€", Money.compact(123_456, MoneyStyle.GREEK))
        assertEquals("<1€", Money.compact(30, MoneyStyle.GREEK))
        assertEquals("12,50", Money.forInput(1_250, MoneyStyle.GREEK))
        assertEquals(123_456L, Money.parseToCents(Money.forInput(123_456, MoneyStyle.GREEK)))
        assertEquals(",", Money.decimalKey(MoneyStyle.GREEK))
    }

    @Test fun `the chosen style is the default`() {
        Money.style = MoneyStyle.GREEK
        assertEquals("12,50 €", Money.format(1_250))
        Money.style = MoneyStyle.ENGLISH
        assertEquals("€12.50", Money.format(1_250))
    }
}

class WordsTest {
    @Test fun `plain words`() {
        val p = Words.PLAIN
        assertEquals("🎯 Goals", p.goalsTab)
        assertEquals("Search", p.search)
        assertEquals("Delete", p.deleteBtn)
        assertEquals("80% of your Fuel goal is spent", p.alert80("Fuel"))
        assertEquals("You've reached your Fuel goal", p.alert100("Fuel"))
        assertEquals("Spent in 2026", p.spentIn(2026))
    }

    @Test fun `settings tab and export labels`() {
        assertEquals("⚙️ Settings", Words.WIZARD.settingsTab)
        assertEquals("⚙️ Settings", Words.PLAIN.settingsTab)
        assertEquals("📜 Geminio", Words.WIZARD.exportTitle)
        assertEquals("Geminio · export CSV", Words.WIZARD.exportButton)
        assertEquals("Export CSV", Words.PLAIN.exportButton)
        assertEquals(5, com.csymvoul.expenses.ui.components.Tab.entries.size)
    }

    @Test fun `wizard words match the earlier wording`() {
        val w = Words.WIZARD
        assertEquals("🦉 80% of your Entertainment ward is spent", w.alert80("Entertainment"))
        assertEquals("⚡ Your Entertainment ward is broken", w.alert100("Entertainment"))
        assertEquals("🦉 Owl post: did you forget the ledger?", w.owlNothing)
    }

    @Test fun `plain words reach reminders, warnings and goal text`() {
        assertEquals("Did you record today's expenses?", Reminder.message(emptyList(), LocalDate.of(2026, 11, 22), Words.PLAIN).title)
        val nov = YearMonth.of(2026, 11)
        val d = AppData(
            listOf(Expense("x", LocalDate.of(2026, 11, 10), "t", 6_500, Category.ENTERTAINMENT)),
            Goals().set(Category.ENTERTAINMENT, nov, 6_000, onwards = false),
        )
        assertEquals("You've reached your Entertainment goal", GoalAlerts.due(d, nov, Words.PLAIN).single().title)
        assertEquals("150% of €20.00 · €10.00 over. Over the goal!", GoalProgress(2_000, 3_000).describe(Words.PLAIN))
    }
}

class SettingsTest {
    @Test fun `defaults`() {
        val s = Settings.fromStored(null, null, null, null, null)
        assertEquals(Settings(), s)
        assertEquals(LocalTime.of(21, 30), s.reminderTime)
        assertTrue(s.wizardWords)
    }

    @Test fun `stored values, and invalid ones fall back`() {
        val s = Settings.fromStored(false, "07:15", false, "GREEK", "DARK")
        assertEquals(Settings(false, LocalTime.of(7, 15), false, MoneyStyle.GREEK, ThemeMode.DARK), s)
        assertEquals(Words.PLAIN, s.words)
        val bad = Settings.fromStored(true, "25:00", true, "XX", "PURPLE")
        assertEquals(Settings(), bad)
    }

    @Test fun `reminder time parsing`() {
        assertEquals(LocalTime.of(0, 0), Reminder.parseTime("00:00"))
        assertEquals(LocalTime.of(23, 59), Reminder.parseTime("23:59"))
        assertNull(Reminder.parseTime("24:00"))
        assertNull(Reminder.parseTime("9:30"))
    }

    @Test fun `next reminder at a chosen time`() {
        val zone = java.time.ZoneId.of("Europe/Athens")
        val now = java.time.ZonedDateTime.of(2026, 11, 22, 20, 0, 0, 0, zone)
        assertEquals(java.time.ZonedDateTime.of(2026, 11, 23, 19, 45, 0, 0, zone), Reminder.nextTrigger(now, LocalTime.of(19, 45)))
    }
}

class SuggestionsTest {
    private fun e(id: String, date: String, title: String, cents: Long, cat: Category, percent: Int = 100, food: FoodType? = null) =
        Expense(id, LocalDate.parse(date), title, cents, cat, percent, food)

    private val history = listOf(
        e("a", "2026-11-01", "Freddo espresso", 250, Category.ENTERTAINMENT, food = FoodType.DAY_TO_DAY),
        e("b", "2026-11-03", "Freddo espresso", 270, Category.ENTERTAINMENT, food = FoodType.DAY_TO_DAY),
        e("c", "2026-11-02", "Fresh juice", 400, Category.ENTERTAINMENT, food = FoodType.DAY_TO_DAY),
        e("d", "2026-11-04", "Diesel", 6_000, Category.FUEL),
        e("e", "2026-11-05", "Σουβλάκι με Μαρία", 3_600, Category.ENTERTAINMENT, 50, FoodType.DINE_OUT),
    )

    @Test fun `most used first, with the latest amount`() {
        val s = Suggestions.of(history, "fre")
        assertEquals(listOf("Freddo espresso", "Fresh juice"), s.map { it.title })
        assertEquals(Suggestion("Freddo espresso", 270, Category.ENTERTAINMENT, FoodType.DAY_TO_DAY, 100, 2), s[0])
    }

    @Test fun `matches words inside the title, ignoring accents`() {
        assertEquals(listOf("Freddo espresso"), Suggestions.of(history, "espr").map { it.title })
        assertEquals(listOf("Σουβλάκι με Μαρία" to 50), Suggestions.of(history, "μαρια").map { it.title to it.myPercent })
    }

    @Test fun `nothing typed or nothing matching`() {
        assertTrue(Suggestions.of(history, "").isEmpty())
        assertTrue(Suggestions.of(history, "xyz").isEmpty())
        assertEquals(1, Suggestions.of(history, "f", limit = 1).size)
    }

    @Test fun `quick add default titles`() {
        assertEquals("Super market", Suggestions.defaultTitle(Category.SUPERMARKET, null))
        assertEquals("Day-to-day", Suggestions.defaultTitle(Category.ENTERTAINMENT, FoodType.DAY_TO_DAY))
        assertEquals("Fuel", Suggestions.defaultTitle(Category.FUEL, null))
    }

    @Test fun `the update check is on unless switched off`() {
        assertEquals(true, com.csymvoul.expenses.data.Settings.fromStored(null, null, null, null, null).updateCheck)
        assertEquals(false, com.csymvoul.expenses.data.Settings.fromStored(null, null, null, null, null, updateCheck = false).updateCheck)
        assertEquals(true, com.csymvoul.expenses.data.Settings().updateCheck)
    }
}
