package com.csymvoul.expenses.domain

import com.csymvoul.expenses.data.ExpenseStore
import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.ui.theme.nickname
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

class MonthRecapTest {
    @get:Rule val tmp = TemporaryFolder()
    private var n = 0
    private fun e(date: String, cents: Long, cat: Category) = Expense("id${n++}", LocalDate.parse(date), "t$n", cents, cat)
    private val data = AppData(
        listOf(
            e("2026-10-01", 45_000, Category.RENT), e("2026-10-05", 9_000, Category.SHOPPING), e("2026-10-20", 70_000, Category.SHOPPING),
            e("2026-09-01", 45_000, Category.RENT), e("2026-09-10", 90_000, Category.SHOPPING),
        ),
    )

    @Test fun `the recap gives the total, the change and where most went, worded as on the web`() {
        val r = MonthRecap.of(data, YearMonth.of(2026, 10))!!
        assertEquals(MonthRecap(YearMonth.of(2026, 10), 124_000, YearMonth.of(2026, 9), -8, Category.SHOPPING, 79_000), r)
        assertEquals("October: €1,240.00, 8% less than September, most went to Diagon Alley.", r.text { it.nickname })
        assertEquals("October: €1,240.00, 8% less than September, most went to Shopping.", r.text { it.label })
        assertEquals("October: €1,240.00, 12% more than September, most went to Shopping.", r.copy(changePercent = 12).text { it.label })
        assertEquals("October: €1,240.00, the same as September, most went to Shopping.", r.copy(changePercent = 0).text { it.label })
        assertEquals("October: €1,240.00, most went to Shopping.", r.copy(changePercent = null).text { it.label })
        assertNull(MonthRecap.of(data, YearMonth.of(2026, 8)))
        assertNull(MonthRecap.of(data, YearMonth.of(2026, 9))!!.changePercent)
        assertEquals(YearMonth.of(2026, 10), MonthRecap.monthFor(LocalDate.of(2026, 11, 1)))
        assertEquals(YearMonth.of(2026, 12), MonthRecap.monthFor(LocalDate.of(2027, 1, 15)))
        assertEquals("🦉 Owl post: October's ledger is closed", Words.WIZARD.recapTitle("October"))
        assertEquals("October summary", Words.PLAIN.recapTitle("October"))
    }

    @Test fun `dismissing the card is saved with the data`() {
        val store = ExpenseStore(File(tmp.root, "expenses.json"))
        store.save(AppData(recapDismissed = "2026-10"))
        assertEquals("2026-10", store.load().recapDismissed)
        store.save(AppData())
        assertNull(store.load().recapDismissed)
    }
}
