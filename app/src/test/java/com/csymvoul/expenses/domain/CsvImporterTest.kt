package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CsvImporterTest {
    private var n = 0
    private val ids = { "new${n++}" }
    private fun e(date: String, title: String, cents: Long, cat: Category = Category.OTHER, pct: Int = 100, food: FoodType? = null) =
        Expense("old$title$date$cents", LocalDate.parse(date), title, cents, cat, pct, food)

    @Test fun `a Geminio export comes back in exactly`() {
        val ledger = listOf(
            e("2026-11-01", "Rent, Nov", 45000, Category.RENT),
            e("2026-11-22", "Coffee \"Freddo\"", 270, Category.ENTERTAINMENT, 50, FoodType.DAY_TO_DAY),
            e("2026-11-03", "Lidl", 6230, Category.SUPERMARKET),
        )
        val csv = CsvExporter.toCsv(ledger)
        val fresh = CsvImporter.import(csv, emptyList(), ids)
        assertEquals(3, fresh.rows)
        assertEquals(emptyList<ImportProblem>(), fresh.problems)
        assertEquals(ledger.sortedBy { it.date }.map { it.copy(id = "") }, fresh.added.map { it.copy(id = "") })
        // Importing the same backup again adds nothing.
        val again = CsvImporter.import(csv, ledger, ids)
        assertEquals(0, again.added.size)
        assertEquals(3, again.duplicates)
    }

    @Test fun `identical rows are matched one for one`() {
        val coffee = e("2026-11-05", "Coffee", 250, Category.ENTERTAINMENT, food = FoodType.DAY_TO_DAY)
        val csv = CsvExporter.toCsv(listOf(coffee, coffee.copy(id = "2"), coffee.copy(id = "3")))
        val r = CsvImporter.import(csv, listOf(coffee), ids)
        assertEquals(1, r.duplicates)
        assertEquals(2, r.added.size)
        assertTrue(r.added.all { it.id.startsWith("new") })
    }

    @Test fun `spreadsheet CSVs work too`() {
        val csv = "\uFEFFDate;Title;Category;Food type;Amount EUR;My share percent\r\n" +
            "22/11/2026;Souvlaki;entertainment;delivery;\"1.234,56\";\r\n" +
            "3.11.2026;Diesel;FUEL;;60;50\r\n\r\n"
        val r = CsvImporter.import(csv, emptyList(), ids)
        assertEquals(emptyList<ImportProblem>(), r.problems)
        assertEquals(2, r.rows)
        val (souvlaki, diesel) = r.added
        assertEquals(LocalDate.of(2026, 11, 22), souvlaki.date)
        assertEquals(FoodType.DELIVERY, souvlaki.foodType)
        assertEquals(123456L, souvlaki.amountCents)
        assertEquals(100, souvlaki.myPercent)
        assertEquals(Category.FUEL, diesel.category)
        assertEquals(50, diesel.myPercent)
    }

    @Test fun `unreadable rows are listed with their line and reason, the rest still come in`() {
        val csv = """
            date,title,category,food_type,amount_eur,my_share_percent
            2026-11-31,Ghost,Rent,,10,100
            2026-11-02,,Rent,,10,100
            2026-11-02,Wands,Potions,,10,100
            2026-11-02,Wine,Bills,Dine out,10,100
            2026-11-02,Socks,Shopping,,ten,100
            2026-11-02,Split,Other,,10,0
            2026-11-02,Good,Other,,10,
        """.trimIndent()
        val r = CsvImporter.import(csv, emptyList(), ids)
        assertEquals(listOf(2, 3, 4, 5, 6, 7), r.problems.map { it.line })
        assertEquals("\"2026-11-31\" is not a date.", r.problems[0].reason)
        assertEquals("Only Entertainment can have a food type.", r.problems[3].reason)
        assertEquals(listOf("Good"), r.added.map { it.title })
        assertEquals(7, r.rows)
    }

    @Test fun `a file without the needed columns is refused`() {
        val r = CsvImporter.import("name,price\nx,1\n", emptyList(), ids)
        assertEquals(0, r.added.size)
        assertEquals("Not an Expensiarmus CSV: no date, title, category, amount_eur column.", r.problems.single().reason)
        assertEquals("The file is empty.", CsvImporter.import("", emptyList(), ids).problems.single().reason)
    }

    @Test fun `dates parse in both orders and must be real`() {
        assertEquals(LocalDate.of(2026, 2, 3), CsvImporter.parseDate("2026-2-3"))
        assertEquals(LocalDate.of(2026, 2, 3), CsvImporter.parseDate("03/02/2026"))
        assertNull(CsvImporter.parseDate("2026-02-30"))
        assertNull(CsvImporter.parseDate("Feb 3"))
    }

    @Test fun `the confirmation sums it up and lists at most five problems`() {
        val bad = (1..7).map { ImportProblem(it + 1, "bad $it") }
        val r = ImportResult(added = listOf(e("2026-11-01", "A", 1)), duplicates = 2, problems = bad, rows = 10)
        assertEquals(
            listOf(
                "Found 10 expenses in the file.", "1 new, 2 already in your ledger.", "7 rows could not be read:",
                "Line 2: bad 1", "Line 3: bad 2", "Line 4: bad 3", "Line 5: bad 4", "Line 6: bad 5", "…and 2 more.",
            ),
            CsvImporter.summary(r),
        )
        assertEquals(listOf("Found 1 expense in the file.", "0 new, 1 already in your ledger."), CsvImporter.summary(ImportResult(emptyList(), 1, emptyList(), 1)))
    }

    @Test fun `apparate is the import spell, with plain words too`() {
        assertEquals("Apparate · import CSV", Words.WIZARD.importButton)
        assertEquals("Apparate! 12 expenses appeared in the ledger.", Words.WIZARD.imported(12))
        assertEquals("1 expense imported.", Words.PLAIN.imported(1))
        assertEquals("Import CSV", Words.PLAIN.importButton)
    }
}
