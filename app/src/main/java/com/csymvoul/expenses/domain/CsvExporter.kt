package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Expense

object CsvExporter {
    const val HEADER = "date,title,category,food_type,amount_eur,my_share_percent,my_share_eur"

    /** Oldest first, one line per expense. */
    fun toCsv(expenses: List<Expense>): String = buildString {
        append(HEADER).append('\n')
        expenses.sortedBy { it.date }.forEach { e ->
            append(
                listOf(
                    e.date.toString(),
                    escape(e.title),
                    escape(e.category.label),
                    e.foodType?.label.orEmpty(),
                    Money.plain(e.amountCents),
                    e.myPercent.toString(),
                    Money.plain(e.myShareCents),
                ).joinToString(","),
            ).append('\n')
        }
    }

    internal fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
