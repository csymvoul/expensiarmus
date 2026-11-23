package com.csymvoul.expenses.widget

import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.domain.MoneyStyle
import com.csymvoul.expenses.domain.Words
import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.Goals
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class WidgetContentTest {
    private val nov = YearMonth.of(2026, 11)
    private var n = 0
    private fun e(cents: Long, cat: Category) = Expense("id${n++}", LocalDate.of(2026, 11, 10), "t", cents, cat)

    @After fun reset() { Money.style = MoneyStyle.ENGLISH }

    @Test fun `total and goals, closest to the goal first, at most three`() {
        val goals = Goals()
            .set(Category.ENTERTAINMENT, nov, 6_000, false)
            .set(Category.FUEL, nov, 10_000, false)
            .set(Category.SUPERMARKET, nov, 20_000, false)
            .set(Category.SHOPPING, nov, 1_000, false)
        val data = AppData(listOf(e(4_800, Category.ENTERTAINMENT), e(2_000, Category.FUEL), e(15_000, Category.SUPERMARKET), e(1_500, Category.SHOPPING)), goals)
        val w = WidgetContent.of(data, nov, Words.WIZARD)
        assertEquals("Expensiarmus · November", w.heading)
        assertEquals("€233.00", w.total)
        assertEquals("Gold spent this month", w.label)
        assertEquals(3, w.wards.size)
        assertEquals(WardLine("🛍️ Shopping · €15.00 of €10.00", 100, true), w.wards[0])
        assertEquals(WardLine("🍽️ Entertainment · €48.00 of €60.00", 80, false), w.wards[1])
        assertEquals(75, w.wards[2].percent)
        assertNull(w.emptyNote)
    }

    @Test fun `no goals shows a note, in the chosen words and number style`() {
        Money.style = MoneyStyle.GREEK
        val w = WidgetContent.of(AppData(listOf(e(1_250, Category.RENT))), nov, Words.PLAIN)
        assertEquals("12,50 €", w.total)
        assertEquals("Spent this month", w.label)
        assertEquals("No goals for this month yet.", w.emptyNote)
        assertEquals(emptyList<WardLine>(), w.wards)
    }
}
