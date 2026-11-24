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

    /** The result of saving on the Favourites page: the new list (sorted by title), or what to fix. */
    data class Saved(val list: List<Favorite>?, val error: String?)

    /** Replaces the favourite at [index], or adds a new one when [index] is null. Same rules as the web app. */
    fun save(favorites: List<Favorite>, index: Int?, fav: Favorite): Saved {
        val f = fav.copy(title = fav.title.trim(), foodType = fav.foodType.takeIf { fav.category == com.csymvoul.expenses.model.Category.ENTERTAINMENT })
        if (f.title.isEmpty()) return Saved(null, "Give it a title.")
        if (f.myPercent !in 1..100) return Saved(null, "Your share must be between 1 and 100%.")
        val others = favorites.filterIndexed { i, _ -> i != index }
        if (others.any { key(it) == key(f) }) return Saved(null, "You already have this favourite.")
        if (index == null && favorites.size >= MAX) return Saved(null, "You can keep up to $MAX favourites.")
        return Saved((others + f).sortedBy { ExpenseQueries.normalize(it.title) }, null)
    }

    fun removeAt(favorites: List<Favorite>, index: Int) = favorites.filterIndexed { i, _ -> i != index }

    /** What loading keeps: valid favourites, at most five. */
    fun cleaned(favorites: List<Favorite>) = favorites.filter { it.isValid }.map { it.copy(title = it.title.trim()) }.take(MAX)
}
