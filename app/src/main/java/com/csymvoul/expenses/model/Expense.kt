package com.csymvoul.expenses.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate
import java.time.YearMonth

/**
 * One expense. [amountCents] is the full amount paid in euro cents; [myPercent] is the
 * part of it that is mine (100 = not split, 50 = split 50-50).
 */
@Serializable
data class Expense(
    val id: String,
    @Serializable(with = LocalDateSerializer::class) val date: LocalDate,
    val title: String,
    val amountCents: Long,
    val category: Category,
    val myPercent: Int = 100,
    /** Set only for food expenses, which are always [Category.ENTERTAINMENT]. */
    val foodType: FoodType? = null,
    /** The trip this expense also belongs to (it keeps its category and month), or null. */
    val tripId: String? = null,
) {
    init {
        require(amountCents >= 0) { "amount must not be negative" }
        require(myPercent in 1..100) { "myPercent must be 1..100" }
        require(foodType == null || category == Category.ENTERTAINMENT) { "only entertainment can be food" }
    }

    val isSplit: Boolean get() = myPercent < 100

    /** My part of the amount, rounded half up to the nearest cent. */
    val myShareCents: Long get() = (amountCents * myPercent + 50) / 100
}

/**
 * One goal for one category, or for the whole month when [category] is null (a goal on everything
 * spent that month). [month] is "2026-11". With [onwards] it applies to that month and
 * every later month (until a later "onwards" goal for the same category); without it, only to
 * that month. [cents] of 0 means "no goal" and is used to switch an earlier goal off.
 */
@Serializable
data class GoalEntry(
    val category: Category? = null,
    val month: String,
    val cents: Long,
    val onwards: Boolean,
)

/**
 * Spending goals per category and month. A goal for one specific month always wins over an
 * "onwards" goal. [monthlyCents] and [perCategoryCents] are the pre-1.5 format (one amount for
 * every month) and are only read to convert old data, see [migrated].
 */
@Serializable
data class Goals(
    val entries: List<GoalEntry> = emptyList(),
    val monthlyCents: Long? = null,
    val perCategoryCents: Map<Category, Long> = emptyMap(),
) {
    /** The goal that applies to [category] (null: the whole month) in [month], or null when there is none. */
    fun goalFor(category: Category?, month: YearMonth): GoalEntry? {
        val key = month.toString()
        val single = entries.firstOrNull { it.category == category && !it.onwards && it.month == key }
        val effective = single ?: entries
            .filter { it.category == category && it.onwards && it.month <= key }
            .maxByOrNull { it.month }
        return effective?.takeIf { it.cents > 0 }
    }

    /** Sets [cents] for [category] in [month] only, or from [month] onwards. */
    fun set(category: Category?, month: YearMonth, cents: Long, onwards: Boolean): Goals {
        val key = month.toString()
        val kept = entries.filterNot {
            it.category == category && ((!it.onwards && it.month == key) || (onwards && it.onwards && it.month >= key))
        }
        return copy(entries = kept + GoalEntry(category, key, cents, onwards))
    }

    /** Removes the goal for [category] in [month] only, or from [month] onwards. */
    fun remove(category: Category?, month: YearMonth, onwards: Boolean): Goals {
        val key = month.toString()
        var kept = entries.filterNot { it.category == category && !it.onwards && it.month == key }
        if (onwards) kept = kept.filterNot { it.category == category && it.month >= key }
        // An earlier "onwards" goal may still reach this month: switch it off explicitly.
        if (Goals(kept).goalFor(category, month) != null) kept = kept + GoalEntry(category, key, 0, onwards)
        return copy(entries = kept)
    }

    /**
     * Converts pre-1.5 goals: the Entertainment goal (the only one used since 1.4.0) becomes an
     * "every month" goal; the monthly total and other categories are dropped.
     */
    fun migrated(): Goals {
        if (monthlyCents == null && perCategoryCents.isEmpty()) return this
        val legacy = perCategoryCents[Category.ENTERTAINMENT]?.takeIf { it > 0 }
            ?.let { listOf(GoalEntry(Category.ENTERTAINMENT, LEGACY_START, it, onwards = true)) }
            .orEmpty()
        return Goals(entries = legacy + entries)
    }

    companion object {
        /** Start month given to converted goals, so they keep covering past months too. */
        const val LEGACY_START = "2000-01"
    }
}

/** A trip: a name, its first and last day, and an optional total budget. Expenses point to it by [id]. */
@Serializable
data class Trip(
    val id: String,
    val name: String,
    @Serializable(with = LocalDateSerializer::class) val start: LocalDate,
    @Serializable(with = LocalDateSerializer::class) val end: LocalDate,
    val budgetCents: Long? = null,
)

/** A starting point for a new expense: title, category, food type and split, never an amount. */
@Serializable
data class Favorite(
    val title: String,
    val category: Category,
    val foodType: FoodType? = null,
    val myPercent: Int = 100,
) {
    /** Usable: a title, food types only under Entertainment, a share of 1–100%. */
    val isValid: Boolean
        get() = title.isNotBlank() && (foodType == null || category == Category.ENTERTAINMENT) && myPercent in 1..100
}

/** Everything the app stores. */
@Serializable
data class AppData(
    val expenses: List<Expense> = emptyList(),
    val goals: Goals = Goals(),
    /** Goal warnings already sent, as "CATEGORY|2026-11|80" (or 100), so each comes once. */
    val alertsSent: Set<String> = emptySet(),
    /** Up to five favourites for Quick add, sorted by title. */
    val favorites: List<Favorite> = emptyList(),
    /** The month ("2026-10") whose recap card was dismissed on the Ledger. */
    val recapDismissed: String? = null,
    /** Trips, newest first. */
    val trips: List<Trip> = emptyList(),
) {
    fun upsert(expense: Expense) = copy(expenses = expenses.filterNot { it.id == expense.id } + expense)

    fun delete(id: String) = copy(expenses = expenses.filterNot { it.id == id })

    fun find(id: String): Expense? = expenses.firstOrNull { it.id == id }
}

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}
