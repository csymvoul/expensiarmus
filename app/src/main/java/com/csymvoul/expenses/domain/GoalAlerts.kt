package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.GoalEntry
import com.csymvoul.expenses.model.Goals
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A warning that a category (or, with [category] null, the whole month) has used 80% of its goal
 * ([level] 80) or reached it ([level] 100).
 */
data class GoalAlert(
    val category: Category?,
    val month: YearMonth,
    val level: Int,
    val goal: GoalProgress,
    val words: Words = Words.WIZARD,
) {
    val title: String
        get() = (category?.label ?: "monthly").let { if (level >= 100) words.alert100(it) else words.alert80(it) }

    val body: String
        get() {
            val head = "${Money.format(goal.spentCents)} of ${Money.format(goal.goalCents)} in ${month.format(MONTH)}"
            return when {
                goal.isOver -> "$head · ${Money.format(-goal.remainingCents)} over."
                level >= 100 -> "$head · nothing left."
                else -> "$head · ${Money.format(goal.remainingCents)} left. Time to slow down."
            }
        }

    companion object {
        private val MONTH = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
    }
}

/** Decides which goal warnings are due, each at most once per category, month and level. */
object GoalAlerts {
    fun key(category: Category?, month: YearMonth, level: Int) = "${category?.name ?: "TOTAL"}|$month|$level"

    /** Warnings not yet sent for [month]: at 80% of a goal, and on reaching it (100%, which wins). */
    fun due(data: AppData, month: YearMonth, words: Words = Words.WIZARD): List<GoalAlert> {
        val s = MonthlySummary.of(data, month)
        val watched = listOf<Pair<Category?, GoalProgress?>>(null to s.totalGoal) + s.categories.map { it.category to it.goal }
        return watched.mapNotNull { (category, goal) ->
            goal ?: return@mapNotNull null
            val level = when {
                goal.spentCents >= goal.goalCents -> 100
                goal.spentCents * 100 >= goal.goalCents * 80 -> 80
                else -> return@mapNotNull null
            }
            if (key(category, month, level) in data.alertsSent) null else GoalAlert(category, month, level, goal, words)
        }
    }

    /** Records [alerts] as sent. Reaching 100% also counts as the 80% warning. */
    fun markSent(data: AppData, alerts: List<GoalAlert>): AppData =
        data.copy(
            alertsSent = data.alertsSent + alerts.flatMap { a ->
                (if (a.level >= 100) listOf(80, 100) else listOf(80)).map { key(a.category, a.month, it) }
            },
        )
}

/** How far a goal reaches, as shown under it on the Protego screen. */
object GoalScope {
    /** A ward's key on screen: the category's code, or "TOTAL" for the whole month. */
    fun key(category: Category?): String = category?.name ?: "TOTAL"

    /** The category a ward key stands for; null for the whole month. */
    fun target(key: String): Category? = if (key == "TOTAL") null else Category.valueOf(key)

    /** What can carry a ward, in screen order: the whole month first, then every category. */
    val targets: List<Category?> = listOf<Category?>(null) + Category.entries

    private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

    fun text(entry: GoalEntry, month: YearMonth): String = when {
        !entry.onwards -> "Only ${month.format(MONTH_YEAR)}"
        entry.month == Goals.LEGACY_START -> "Every month"
        entry.month == month.toString() -> "From ${month.format(MONTH_YEAR)} onwards"
        else -> "Every month since ${YearMonth.parse(entry.month).format(MONTH_YEAR)}"
    }
}
