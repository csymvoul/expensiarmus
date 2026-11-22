package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTest {
    private val athens = ZoneId.of("Europe/Athens")
    private fun at(text: String) = ZonedDateTime.of(LocalDateTime.parse(text), athens)

    @Test fun `before 21_30 fires today`() =
        assertEquals(at("2026-11-22T21:30"), Reminder.nextTrigger(at("2026-11-22T09:00")))

    @Test fun `at or after 21_30 fires tomorrow`() {
        assertEquals(at("2026-11-23T21:30"), Reminder.nextTrigger(at("2026-11-22T21:30")))
        assertEquals(at("2026-11-23T21:30"), Reminder.nextTrigger(at("2026-11-22T23:59")))
    }

    @Test fun `end of month and year roll over`() =
        assertEquals(at("2027-01-01T21:30"), Reminder.nextTrigger(at("2026-12-31T22:00")))

    @Test fun `stays at 21_30 local time across the clock change`() {
        // Greece goes back to winter time on 25 Oct 2026.
        val next = Reminder.nextTrigger(at("2026-10-24T22:00"))
        assertEquals(at("2026-10-25T21:30"), next)
        assertEquals(21, next.hour)
        assertEquals(30, next.minute)
    }

    private val today = LocalDate.of(2026, 11, 22)
    private fun e(date: LocalDate, cents: Long, percent: Int = 100) =
        Expense(date.toString() + cents, date, "x", cents, Category.OTHER, percent)

    @Test fun `nothing recorded today`() {
        val m = Reminder.message(listOf(e(today.minusDays(1), 500)), today)
        assertEquals("Did you forget the ledger?", m.title)
        assertEquals("No expenses recorded today. Tap to record what you spent.", m.text)
    }

    @Test fun `some expenses today uses my share`() {
        val m = Reminder.message(listOf(e(today, 1_000), e(today, 3_000, 50), e(today.minusDays(1), 9_999)), today)
        assertEquals("Anything else for the ledger?", m.title)
        assertEquals("Today: 2 expenses, €25.00. Tap to add anything you missed.", m.text)
    }

    @Test fun `one expense is singular`() =
        assertEquals("Today: 1 expense, €4.50. Tap to add anything you missed.", Reminder.message(listOf(e(today, 450)), today).text)
}
