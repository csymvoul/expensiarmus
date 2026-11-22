package com.csymvoul.expenses.ui

import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.ui.theme.emoji
import com.csymvoul.expenses.ui.theme.nickname
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmblemsTest {
    @Test fun `every category has its own emblem and nickname`() {
        assertEquals(Category.entries.size, Category.entries.map { it.emoji }.toSet().size)
        assertEquals(Category.entries.size, Category.entries.map { it.nickname }.toSet().size)
        assertTrue(Category.entries.all { it.emoji.isNotBlank() && it.nickname.isNotBlank() })
    }

    @Test fun `every food type has its own emblem, different from the categories`() {
        val food = FoodType.entries.map { it.emoji }.toSet()
        assertEquals(FoodType.entries.size, food.size)
        assertTrue(food.none { it in Category.entries.map { c -> c.emoji } })
        assertEquals(FoodType.entries.size, FoodType.entries.map { it.nickname }.toSet().size)
    }

    @Test fun `shopping is diagon alley with a shopping bag`() {
        assertEquals("Diagon Alley", Category.SHOPPING.nickname)
        assertEquals("🛍️", Category.SHOPPING.emoji)
    }

    @Test fun `fuel is floo powder at the pump`() {
        assertEquals("⛽", Category.FUEL.emoji)
        assertEquals("Floo Powder", Category.FUEL.nickname)
    }

    @Test fun `entertainment shows food`() = assertEquals("🍽️", Category.ENTERTAINMENT.emoji)
}
