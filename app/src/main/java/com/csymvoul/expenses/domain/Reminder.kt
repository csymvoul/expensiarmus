package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Expense
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** When the evening reminder fires and what it says. */
object Reminder {
    val TIME: LocalTime = LocalTime.of(21, 30)

    /** The next [time] strictly after [now]: today if it is still ahead, otherwise tomorrow. */
    fun nextTrigger(now: ZonedDateTime, time: LocalTime = TIME): ZonedDateTime {
        val today = now.toLocalDate().atTime(time).atZone(now.zone)
        return if (now.isBefore(today)) today else now.toLocalDate().plusDays(1).atTime(time).atZone(now.zone)
    }

    /**
     * Exact alarms fire at 21:30 even when the phone is idle; inexact ones may arrive hours late.
     * Before Android 12 every app may use them; from Android 12 only when the system allows it.
     */
    fun useExactAlarm(sdk: Int, canScheduleExact: Boolean): Boolean = sdk < 31 || canScheduleExact

    /** "21:30" → 21:30; null for anything else. */
    fun parseTime(text: String): LocalTime? =
        Regex("^([01]\\d|2[0-3]):([0-5]\\d)$").matchEntire(text)?.let { LocalTime.of(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }

    data class Message(val title: String, val text: String)

    fun message(expenses: List<Expense>, today: LocalDate, words: Words = Words.WIZARD): Message {
        val todays = expenses.filter { it.date == today }
        if (todays.isEmpty()) {
            return Message(words.owlNothing, "No expenses recorded today. Tap to record what you spent.")
        }
        val count = if (todays.size == 1) "1 expense" else "${todays.size} expenses"
        return Message(
            words.owlSome,
            "Today: $count, ${Money.format(todays.sumOf { it.myShareCents })}. Tap to add anything you missed.",
        )
    }
}
