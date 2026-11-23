package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import java.time.YearMonth

data class GoalProgress(val goalCents: Long, val spentCents: Long) {
    /** Spent / goal; above 1 when over the goal. */
    val fraction: Float get() = if (goalCents > 0) spentCents.toFloat() / goalCents else 0f

    /** Spent as a whole percentage of the goal, rounded half up. */
    val percent: Int get() = if (goalCents > 0) ((spentCents * 100 + goalCents / 2) / goalCents).toInt() else 0

    /** Negative when over the goal. */
    val remainingCents: Long get() = goalCents - spentCents

    val isOver: Boolean get() = spentCents > goalCents

    /** "63% of €1,200.00 · €444.00 left", or the overspend with a broken ward. */
    fun describe(words: Words = Words.WIZARD): String {
        val head = "$percent% of ${Money.format(goalCents)}"
        return if (isOver) {
            "$head · ${Money.format(-remainingCents)} over. ${words.overGoal}"
        } else {
            "$head · ${Money.format(remainingCents)} left"
        }
    }
}

data class CategoryTotal(
    val category: Category,
    val spentCents: Long,
    /** This category's part of the month's total, 0..1. */
    val shareOfMonth: Float,
    val goal: GoalProgress?,
)

data class FoodTotal(
    val type: FoodType,
    val spentCents: Long,
    val count: Int,
    /** This type's part of all food spending, 0..1. */
    val shareOfFood: Float,
)

/** Food spending inside the Entertainment category. */
data class FoodSummary(
    val totalCents: Long,
    /** One entry per [FoodType], in enum order. */
    val types: List<FoodTotal>,
    /** Entertainment that is not food (cinema, theatre, ...). */
    val otherEntertainmentCents: Long,
) {
    companion object {
        fun of(expenses: List<Expense>): FoodSummary {
            val entertainment = expenses.filter { it.category == Category.ENTERTAINMENT }
            val food = entertainment.filter { it.foodType != null }
            val total = food.sumOf { it.myShareCents }
            return FoodSummary(
                totalCents = total,
                types = FoodType.entries.map { type ->
                    val ofType = food.filter { it.foodType == type }
                    val spent = ofType.sumOf { it.myShareCents }
                    FoodTotal(type, spent, ofType.size, if (total > 0) spent.toFloat() / total else 0f)
                },
                otherEntertainmentCents = entertainment.filter { it.foodType == null }.sumOf { it.myShareCents },
            )
        }
    }
}

/** Totals for one month. All amounts are my share of each expense. */
data class MonthlySummary(
    val month: YearMonth,
    val totalCents: Long,
    /** Full amounts paid, including other people's part of split expenses. */
    val paidTotalCents: Long,
    /** Newest first. */
    val expenses: List<Expense>,
    /** One entry per category, in [Category] order. */
    val categories: List<CategoryTotal>,
    val food: FoodSummary,
) {
    companion object {
        fun of(data: AppData, month: YearMonth): MonthlySummary {
            val inMonth = data.expenses
                .filter { YearMonth.from(it.date) == month }
                .sortedWith(compareByDescending<Expense> { it.date }.thenBy { it.title.lowercase() })
            val total = inMonth.sumOf { it.myShareCents }
            val categories = Category.entries.map { category ->
                val spent = inMonth.filter { it.category == category }.sumOf { it.myShareCents }
                CategoryTotal(
                    category = category,
                    spentCents = spent,
                    shareOfMonth = if (total > 0) spent.toFloat() / total else 0f,
                    goal = data.goals.goalFor(category, month)?.let { GoalProgress(it.cents, spent) },
                )
            }
            return MonthlySummary(
                month = month,
                totalCents = total,
                paidTotalCents = inMonth.sumOf { it.amountCents },
                expenses = inMonth,
                categories = categories,
                food = FoodSummary.of(inMonth),
            )
        }
    }
}
