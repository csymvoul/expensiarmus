package com.csymvoul.expenses.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalDate

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
 * Spending goals. They are not tied to a month: they apply to every month until changed.
 * A null or missing value means no goal.
 */
@Serializable
data class Goals(
    val monthlyCents: Long? = null,
    val perCategoryCents: Map<Category, Long> = emptyMap(),
) {
    companion object {
        /** Categories that can have their own goal. Goals saved for others are ignored. */
        val CATEGORIES_WITH_GOALS = listOf(Category.ENTERTAINMENT)
    }
}

/** Everything the app stores. */
@Serializable
data class AppData(
    val expenses: List<Expense> = emptyList(),
    val goals: Goals = Goals(),
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
