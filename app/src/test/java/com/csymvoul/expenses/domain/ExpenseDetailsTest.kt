package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ExpenseDetailsTest {
    private fun e(cents: Long, percent: Int) = Expense("x", LocalDate.of(2026, 11, 22), "Dinner", cents, Category.ENTERTAINMENT, percent)

    @Test fun `unsplit expense`() = assertEquals("Not split · all yours", ExpenseDetails.splitLine(e(3_600, 100)))

    @Test fun `50-50 split`() =
        assertEquals("Your share 50% (€18.00) · theirs 50% (€18.00)", ExpenseDetails.splitLine(e(3_600, 50)))

    @Test fun `uneven split adds up to the full amount`() =
        // 33% of €10.01 is €3.30 after rounding, so theirs is the remaining €6.71.
        assertEquals("Your share 33% (€3.30) · theirs 67% (€6.71)", ExpenseDetails.splitLine(e(1_001, 33)))
}
