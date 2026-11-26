package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.model.Trip
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Before, during (day [day] of [days]) or after a trip. */
data class TripStatus(val state: State, val days: Int, val day: Int, val inDays: Int = 0) {
    enum class State { UPCOMING, ONGOING, DONE }
}

/** One row of a trip's spend per category: a category, or a food type under Entertainment. */
data class TripCategory(val category: Category, val foodType: FoodType?, val cents: Long) {
    val label: String get() = foodType?.label ?: category.label
}

/** Everything about one trip, as on its page. */
data class TripSummary(
    val trip: Trip,
    val status: TripStatus,
    /** Newest first. */
    val expenses: List<Expense>,
    val totalCents: Long,
    val budget: GoalProgress?,
    val perDayCents: Long,
    val byCategory: List<TripCategory>,
    /** Newest day first. */
    val byDay: List<Pair<LocalDate, Long>>,
)

/**
 * Trips: expenses keep their category and month and can also belong to a trip, so they still count in
 * the month, the category and the wards. Same rules as the web app (web/js/trips.js).
 */
object Trips {
    data class Saved(val list: List<Trip>?, val error: String?)

    /** The trip cleaned up (trimmed name, a 0 budget means none), or what to fix. */
    fun check(t: Trip): Pair<Trip?, String?> {
        val name = t.name.trim()
        return when {
            name.isEmpty() -> null to "Give it a name."
            t.end < t.start -> null to "The last day is before the first."
            (t.budgetCents ?: 0) < 0 -> null to "The budget must be an amount, or left empty."
            else -> t.copy(name = name, budgetCents = t.budgetCents?.takeIf { it > 0 }) to null
        }
    }

    /** Adds or replaces (same id) a trip; newest first. Two trips can't share a name. */
    fun save(trips: List<Trip>, t: Trip): Saved {
        val (trip, error) = check(t)
        if (trip == null) return Saved(null, error)
        val others = trips.filterNot { it.id == t.id }
        if (others.any { ExpenseQueries.normalize(it.name) == ExpenseQueries.normalize(trip.name) }) {
            return Saved(null, "You already have a trip with that name.")
        }
        return Saved((others + trip).sortedWith(compareByDescending<Trip> { it.start }.thenBy { it.name }), null)
    }

    /** Removes a trip; its expenses stay, no longer tied to it. */
    fun delete(data: AppData, id: String): AppData = data.copy(
        trips = data.trips.filterNot { it.id == id },
        expenses = data.expenses.map { if (it.tripId == id) it.copy(tripId = null) else it },
    )

    fun status(t: Trip, today: LocalDate): TripStatus {
        val days = ChronoUnit.DAYS.between(t.start, t.end).toInt() + 1
        return when {
            today < t.start -> TripStatus(TripStatus.State.UPCOMING, days, 0, ChronoUnit.DAYS.between(today, t.start).toInt())
            today > t.end -> TripStatus(TripStatus.State.DONE, days, days)
            else -> TripStatus(TripStatus.State.ONGOING, days, ChronoUnit.DAYS.between(t.start, today).toInt() + 1)
        }
    }

    /** Trips you can still record for: the ones not over yet, the soonest first. */
    fun toOffer(trips: List<Trip>, today: LocalDate) = trips.filter { it.end >= today }.sortedBy { it.start }

    /** The trip on today, if any (the one that started last when they overlap). */
    fun ongoing(trips: List<Trip>, today: LocalDate) = toOffer(trips, today).lastOrNull { it.start <= today }

    fun withoutTrips(expenses: List<Expense>) = expenses.filter { it.tripId == null }

    fun summary(data: AppData, id: String, today: LocalDate): TripSummary? {
        val trip = data.trips.firstOrNull { it.id == id } ?: return null
        val expenses = data.expenses.filter { it.tripId == id }.sortedWith(compareByDescending<Expense> { it.date }.thenBy { it.title })
        val total = expenses.sumOf { it.myShareCents }
        val status = status(trip, today)
        val counted = if (status.state == TripStatus.State.ONGOING) status.day else status.days
        val byCategory = expenses.groupBy { it.category to it.foodType }
            .map { (k, list) -> TripCategory(k.first, k.second, list.sumOf { it.myShareCents }) }
            .sortedWith(compareByDescending<TripCategory> { it.cents }.thenBy { it.label })
        val byDay = expenses.groupBy { it.date }.map { (d, list) -> d to list.sumOf { it.myShareCents } }.sortedByDescending { it.first }
        return TripSummary(
            trip = trip,
            status = status,
            expenses = expenses,
            totalCents = total,
            budget = trip.budgetCents?.let { GoalProgress(it, total) },
            perDayCents = if (counted > 0) (total * 2 + counted) / (counted * 2L) else 0,
            byCategory = byCategory,
            byDay = byDay,
        )
    }

    /** What loading keeps: valid trips with unique ids; expenses tied to a missing trip lose the tie. */
    fun cleaned(data: AppData): AppData {
        val seen = mutableSetOf<String>()
        val trips = data.trips.mapNotNull { t -> if (t.id.isBlank() || !seen.add(t.id)) null else check(t).first }
        val ids = trips.map { it.id }.toSet()
        return data.copy(trips = trips, expenses = data.expenses.map { if (it.tripId != null && it.tripId !in ids) it.copy(tripId = null) else it })
    }
}
