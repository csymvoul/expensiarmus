package com.csymvoul.expenses.ui

import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.R
import com.csymvoul.expenses.ui.components.Tab
import com.csymvoul.expenses.ui.theme.emoji
import com.csymvoul.expenses.ui.theme.icon
import com.csymvoul.expenses.ui.theme.nickname
import androidx.compose.ui.graphics.Color
import com.csymvoul.expenses.domain.Words
import com.csymvoul.expenses.ui.theme.Burgundy
import com.csymvoul.expenses.ui.theme.frameLine
import com.csymvoul.expenses.ui.theme.searchGlyph
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

    @Test fun `every category and food type has its own painted emblem`() {
        val icons = Category.entries.map { it.icon } + FoodType.entries.map { it.icon }
        assertEquals(Category.entries.size + FoodType.entries.size, icons.toSet().size)
        assertEquals(R.drawable.emblem_cat_fuel, Category.FUEL.icon)
        assertEquals(R.drawable.emblem_food_delivery, FoodType.DELIVERY.icon)
    }

    @Test fun `every tab has its own painted emblem, different from the categories`() {
        val tabs = Tab.entries.map { it.icon }.toSet()
        assertEquals(Tab.entries.size, tabs.size)
        assertTrue(tabs.none { it in Category.entries.map { c -> c.icon } + FoodType.entries.map { f -> f.icon } })
        assertEquals(R.drawable.emblem_tab_goals, Tab.GOALS.icon)
    }

    @Test fun `labels lose their leading emoji where a painted emblem stands in`() {
        assertEquals("Settings", Words.withoutEmoji("⚙️ Settings"))
        assertEquals("Annals", Words.withoutEmoji(Words.WIZARD.annals))
        assertEquals("Owl reminder", Words.withoutEmoji(Words.WIZARD.reminder))
        assertEquals("Geminio", Words.withoutEmoji(Words.WIZARD.exportTitle))
        assertEquals("Daily reminder", Words.withoutEmoji("Daily reminder"))
        assertEquals("Protego · November", Words.withoutEmoji("Protego · November"))
    }

    @Test fun `logo and settings emblems are their own pictures`() {
        val icons = listOf(
            R.drawable.emblem_logo_pouch, R.drawable.emblem_search_light, R.drawable.emblem_search_dark, R.drawable.emblem_set_geminio, R.drawable.emblem_set_apparate, R.drawable.emblem_set_reminder,
            R.drawable.emblem_set_theme_system, R.drawable.emblem_set_theme_light, R.drawable.emblem_set_theme_dark,
        )
        assertEquals(icons.size, icons.toSet().size)
        assertTrue(icons.none { it in Tab.entries.map { t -> t.icon } + Category.entries.map { c -> c.icon } })
    }

    @Test fun `framed buttons have gold lines by day and burgundy lines at night`() {
        val gold = Color(0xFFD9A441)
        assertEquals(gold, frameLine(night = false, gold = gold))
        assertEquals(Burgundy, frameLine(night = true, gold = gold))
        assertEquals(Color(0xFF7B1E1E), Burgundy)
    }

    @Test fun `the magnifier is gold at night and bronze by day`() {
        assertEquals(R.drawable.emblem_search_dark, searchGlyph(night = true))
        assertEquals(R.drawable.emblem_search_light, searchGlyph(night = false))
    }
}
