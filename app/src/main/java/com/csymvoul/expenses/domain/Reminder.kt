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

    data class Message(val title: String, val text: String)

    fun message(expenses: List<Expense>, today: LocalDate): Message {
        val todays = expenses.filter { it.date == today }
        if (todays.isEmpty()) {
            return Message("Did you forget the ledger?", "No expenses recorded today. Tap to record what you spent.")
        }
        val count = if (todays.size == 1) "1 expense" else "${todays.size} expenses"
        return Message(
            "Anything else for the ledger?",
            "Today: $count, ${Money.format(todays.sumOf { it.myShareCents })}. Tap to add anything you missed.",
        )
    }
}
