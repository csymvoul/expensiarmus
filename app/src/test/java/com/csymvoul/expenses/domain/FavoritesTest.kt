package com.csymvoul.expenses.domain

import com.csymvoul.expenses.R
import com.csymvoul.expenses.data.ExpenseStore
import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.Favorite
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.ui.theme.starGlyph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate

class FavoritesTest {
    @get:Rule val tmp = TemporaryFolder()
    private var n = 0
    private fun e(title: String, cents: Long = 100, cat: Category = Category.OTHER, pct: Int = 100, food: FoodType? = null) =
        Expense("id${n++}", LocalDate.of(2026, 11, 1), title, cents, cat, pct, food)

    @Test fun `a favourite keeps the title, category, food type and split, never the amount`() {
        val coffee = e(" Freddo espresso ", 270, Category.ENTERTAINMENT, 50, FoodType.DAY_TO_DAY)
        assertEquals(Favorite("Freddo espresso", Category.ENTERTAINMENT, FoodType.DAY_TO_DAY, 50), Favorites.of(coffee))
        var favs = Favorites.toggle(emptyList(), coffee)
        assertTrue(Favorites.isFavorite(favs, coffee.copy(title = "FREDDO ESPRESSO", amountCents = 999)))
        assertFalse(Favorites.isFavorite(favs, coffee.copy(foodType = FoodType.DINE_OUT)))
        favs = Favorites.toggle(favs, e("Apartment", cat = Category.RENT))
        assertEquals(listOf("Apartment", "Freddo espresso"), favs.map { it.title })
        favs = Favorites.toggle(favs, coffee)
        assertEquals(listOf("Apartment"), favs.map { it.title })
    }

    @Test fun `there can be at most five favourites`() {
        assertEquals(5, Favorites.MAX)
        var favs = emptyList<Favorite>()
        for (t in listOf("A", "B", "C", "D", "E")) favs = Favorites.toggle(favs, e(t))
        assertEquals(5, favs.size)
        assertSame(favs, Favorites.toggle(favs, e("F")))
        val fewer = Favorites.toggle(favs, e("C"))
        assertEquals(5, Favorites.toggle(fewer, e("F")).size)
    }

    @Test fun `favourites are saved and loaded, broken ones and any beyond five dropped`() {
        val store = ExpenseStore(File(tmp.root, "expenses.json"))
        val favs = listOf(
            Favorite("Freddo", Category.ENTERTAINMENT, FoodType.DAY_TO_DAY), Favorite("Wine", Category.BILLS, FoodType.DINE_OUT),
            Favorite("  ", Category.OTHER), Favorite("Split", Category.OTHER, myPercent = 0),
            Favorite("A", Category.OTHER), Favorite("B", Category.OTHER), Favorite("C", Category.OTHER), Favorite("D", Category.OTHER), Favorite("E", Category.OTHER),
        )
        store.save(AppData(favorites = favs))
        assertEquals(listOf("Freddo", "A", "B", "C", "D"), store.load().favorites.map { it.title })
        // Data saved before favourites existed loads with none.
        File(tmp.root, "old.json").writeText("""{"expenses":[],"goals":{"entries":[]},"alertsSent":[]}""")
        assertEquals(emptyList<Favorite>(), ExpenseStore(File(tmp.root, "old.json")).load().favorites)
    }

    @Test fun `the star is filled for a favourite, bronze by day and gold at night`() {
        assertEquals(R.drawable.emblem_star_on_light, starGlyph(on = true, night = false))
        assertEquals(R.drawable.emblem_star_on_dark, starGlyph(on = true, night = true))
        assertEquals(R.drawable.emblem_star_off_light, starGlyph(on = false, night = false))
        assertEquals(R.drawable.emblem_star_off_dark, starGlyph(on = false, night = true))
    }
}
