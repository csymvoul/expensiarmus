package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CsvExporterTest {
    @Test fun `writes header and rows oldest first`() {
        val csv = CsvExporter.toCsv(
            listOf(
                Expense("2", LocalDate.of(2026, 11, 5), "Pizza, beers", 2_400, Category.ENTERTAINMENT, 50, FoodType.DELIVERY),
                Expense("1", LocalDate.of(2026, 11, 1), "Rent", 60_000, Category.RENT),
            ),
        )
        assertEquals(
            CsvExporter.HEADER + "\n" +
                "2026-11-01,Rent,Rent,,600.00,100,600.00\n" +
                "2026-11-05,\"Pizza, beers\",Entertainment,Delivery,24.00,50,12.00\n",
            csv,
        )
    }

    @Test fun `empty list writes only the header`() =
        assertEquals(CsvExporter.HEADER + "\n", CsvExporter.toCsv(emptyList()))

    @Test fun `escapes quotes and new lines`() {
        assertEquals("plain", CsvExporter.escape("plain"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escape("say \"hi\""))
        assertEquals("\"a\nb\"", CsvExporter.escape("a\nb"))
    }

    @Test fun `keeps greek text`() {
        val csv = CsvExporter.toCsv(listOf(Expense("1", LocalDate.of(2026, 1, 2), "Δώρο", 1_000, Category.OTHER)))
        assertEquals("2026-01-02,Δώρο,Other,,10.00,100,10.00", csv.lines()[1])
    }
}
