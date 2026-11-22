package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Expense

/** Wording for the read-only expense screen. */
object ExpenseDetails {
    /** "Not split · all yours", or "Your share 50% (€18.00) · theirs 50% (€18.00)". */
    fun splitLine(e: Expense): String {
        if (!e.isSplit) return "Not split · all yours"
        val theirs = e.amountCents - e.myShareCents
        return "Your share ${e.myPercent}% (${Money.format(e.myShareCents)}) · theirs ${100 - e.myPercent}% (${Money.format(theirs)})"
    }
}
