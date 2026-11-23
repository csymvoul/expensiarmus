package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType

/** An earlier expense offered while typing a title, carrying its latest amount, category and split. */
data class Suggestion(
    val title: String,
    val amountCents: Long,
    val category: Category,
    val foodType: FoodType?,
    val myPercent: Int,
    val uses: Int,
)

object Suggestions {
    /**
     * Earlier expenses whose title starts with (or has a word starting with) what was typed, most
     * used first, then most recent. Case and Greek accents are ignored. Same rules as the web app.
     */
    fun of(expenses: List<Expense>, typed: String, limit: Int = 4): List<Suggestion> {
        val q = ExpenseQueries.normalize(typed)
        if (q.isEmpty()) return emptyList()
        data class Group(var uses: Int = 0, var latest: Expense? = null, var at: Int = -1)
        val groups = linkedMapOf<String, Group>()
        expenses.forEachIndexed { i, e ->
            val g = groups.getOrPut(ExpenseQueries.normalize(e.title)) { Group() }
            g.uses++
            val l = g.latest
            if (l == null || e.date > l.date || (e.date == l.date && i > g.at)) {
                g.latest = e
                g.at = i
            }
        }
        return groups.entries
            .filter { (key, _) -> key.startsWith(q) || key.split(Regex("\\s+")).any { it.startsWith(q) } }
            .sortedWith(compareByDescending<Map.Entry<String, Group>> { it.value.uses }.thenByDescending { it.value.latest!!.date }.thenBy { it.key })
            .take(limit)
            .map { (_, g) ->
                val e = g.latest!!
                Suggestion(e.title, e.amountCents, e.category, e.foodType, e.myPercent, g.uses)
            }
    }

    /** Title used when Quick add saves without one: the food type, or else the category. */
    fun defaultTitle(category: Category, foodType: FoodType?): String = foodType?.label ?: category.label
}
