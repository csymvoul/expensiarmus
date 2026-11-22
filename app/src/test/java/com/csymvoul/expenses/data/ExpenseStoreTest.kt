package com.csymvoul.expenses.data

import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.model.Goals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class ExpenseStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val sample = AppData(
        expenses = listOf(
            Expense("a", LocalDate.of(2026, 11, 22), "Σούπερ μάρκετ", 4_567, Category.SUPERMARKET),
            Expense("b", LocalDate.of(2026, 11, 1), "Electricity", 8_000, Category.BILLS, 50),
            Expense("c", LocalDate.of(2026, 11, 2), "Souvlaki", 1_800, Category.ENTERTAINMENT, 50, FoodType.DELIVERY),
        ),
        goals = Goals(monthlyCents = 120_000, perCategoryCents = mapOf(Category.ENTERTAINMENT to 20_000)),
    )

    @Test fun `missing file loads as empty`() {
        assertEquals(AppData(), ExpenseStore(File(tmp.root, "none.json")).load())
    }

    @Test fun `saved data loads back identically`() {
        val file = File(tmp.root, "expenses.json")
        ExpenseStore(file).save(sample)
        assertEquals(sample, ExpenseStore(file).load())
        assertTrue(tmp.root.listFiles()!!.none { it.name.endsWith(".tmp") })
    }

    @Test fun `saving twice overwrites`() {
        val file = File(tmp.root, "expenses.json")
        val store = ExpenseStore(file)
        store.save(sample)
        store.save(sample.delete("a"))
        assertEquals(listOf("b", "c"), store.load().expenses.map { it.id })
    }

    @Test fun `ids and default split survive a round trip`() {
        val file = File(tmp.root, "expenses.json")
        ExpenseStore(file).save(sample)
        val text = file.readText()
        assertTrue(text.contains("\"id\":\"a\""))
        assertTrue(text.contains("\"myPercent\":100"))
    }

    @Test fun `corrupt file is kept as a backup and loads as empty`() {
        val file = File(tmp.root, "expenses.json")
        file.writeText("{not json")
        assertEquals(AppData(), ExpenseStore(file).load())
        val backups = tmp.root.listFiles()!!.filter { it.name.startsWith("expenses.json.corrupt-") }
        assertEquals(1, backups.size)
        assertEquals("{not json", backups.single().readText())
    }

    @Test fun `files saved before food types existed still load`() {
        val file = File(tmp.root, "expenses.json")
        file.writeText(
            """{"expenses":[{"id":"x","date":"2026-11-01","title":"Cinema","amountCents":900,""" +
                """"category":"ENTERTAINMENT","myPercent":100}],"goals":{}}""",
        )
        assertEquals(null, ExpenseStore(file).load().find("x")!!.foodType)
    }

    @Test fun `unknown fields from a newer version are ignored`() {
        val file = File(tmp.root, "expenses.json")
        file.writeText("""{"expenses":[],"goals":{"monthlyCents":500},"theme":"dark"}""")
        assertEquals(500L, ExpenseStore(file).load().goals.monthlyCents)
    }
}
