package com.csymvoul.expenses.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExpenseTest {
    private fun expense(cents: Long, percent: Int = 100, id: String = "a") =
        Expense(id, LocalDate.of(2026, 11, 1), "Dinner", cents, Category.ENTERTAINMENT, percent)

    @Test fun `unsplit expense is all mine`() {
        val e = expense(4_250)
        assertFalse(e.isSplit)
        assertEquals(4_250, e.myShareCents)
    }

    @Test fun `50-50 split halves the amount`() {
        val e = expense(4_250, 50)
        assertTrue(e.isSplit)
        assertEquals(2_125, e.myShareCents)
    }

    @Test fun `split share rounds half up to the cent`() {
        assertEquals(33, expense(101, 33).myShareCents)    // 33.33c -> 33c
        assertEquals(1, expense(1, 50).myShareCents)   // 0.5c -> 1c
        assertEquals(330, expense(1_000, 33).myShareCents) // 33% of €10 = €3.30
        assertEquals(5_001, expense(10_001, 50).myShareCents) // €50.005 -> €50.01
    }

    @Test(expected = IllegalArgumentException::class)
    fun `percent above 100 is rejected`() { expense(100, 101) }

    @Test(expected = IllegalArgumentException::class)
    fun `percent of 0 is rejected`() { expense(100, 0) }

    @Test(expected = IllegalArgumentException::class)
    fun `negative amount is rejected`() { expense(-1) }

    @Test fun `entertainment can be food`() {
        val e = Expense("f", LocalDate.of(2026, 11, 1), "Coffee", 250, Category.ENTERTAINMENT, foodType = FoodType.DAY_TO_DAY)
        assertEquals(FoodType.DAY_TO_DAY, e.foodType)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `other categories cannot be food`() {
        Expense("f", LocalDate.of(2026, 11, 1), "Bread", 250, Category.SUPERMARKET, foodType = FoodType.DELIVERY)
    }

    @Test fun `shopping then fuel come just before other`() {
        assertEquals("Shopping", Category.SHOPPING.label)
        assertEquals("Transport", Category.FUEL.label)
        assertEquals(Category.FUEL.ordinal - 1, Category.SHOPPING.ordinal)
        assertEquals(Category.OTHER.ordinal - 1, Category.FUEL.ordinal)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fuel cannot be food`() {
        Expense("f", LocalDate.of(2026, 11, 1), "Diesel", 6_000, Category.FUEL, foodType = FoodType.DELIVERY)
    }

    @Test fun `upsert adds then replaces by id, delete removes`() {
        var data = AppData()
        data = data.upsert(expense(100, id = "x"))
        data = data.upsert(expense(200, id = "y"))
        data = data.upsert(expense(300, id = "x"))
        assertEquals(2, data.expenses.size)
        assertEquals(300, data.find("x")!!.amountCents)
        data = data.delete("x")
        assertNull(data.find("x"))
        assertEquals(listOf("y"), data.expenses.map { it.id })
    }
}
