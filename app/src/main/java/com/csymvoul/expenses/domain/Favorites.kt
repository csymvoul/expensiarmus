package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.Favorite

/** Favourites: an expense's title, category, food type and split, kept to start new expenses from. Same rules as the web app. */
object Favorites {
    /** Up to five favourites: they fit in one row of Quick add. */
    const val MAX = 5

    fun of(e: Expense) = Favorite(e.title.trim(), e.category, e.foodType, e.myPercent)

    /** Same title (ignoring case and accents), category and food type: the same favourite. */
    fun key(f: Favorite) = listOf(ExpenseQueries.normalize(f.title), f.category.name, f.foodType?.name.orEmpty()).joinToString("|")

    fun isFavorite(favorites: List<Favorite>, e: Expense) = favorites.any { key(it) == key(of(e)) }

    /**
     * Stars or un-stars an expense's title, category and food type; favourites stay sorted by title.
     * With five already, starring another returns the same list (the screen says why).
     */
    fun toggle(favorites: List<Favorite>, e: Expense): List<Favorite> = toggle(favorites, of(e))

    fun toggle(favorites: List<Favorite>, f: Favorite): List<Favorite> = when {
        favorites.any { key(it) == key(f) } -> favorites.filterNot { key(it) == key(f) }
        favorites.size >= MAX -> favorites
        else -> (favorites + f).sortedBy { ExpenseQueries.normalize(it.title) }
    }

    /** What loading keeps: valid favourites, at most five. */
    fun cleaned(favorites: List<Favorite>) = favorites.filter { it.isValid }.map { it.copy(title = it.title.trim()) }.take(MAX)
}
