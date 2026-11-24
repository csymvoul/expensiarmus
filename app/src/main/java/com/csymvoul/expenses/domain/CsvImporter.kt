package com.csymvoul.expenses.domain

import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import java.time.DateTimeException
import java.time.LocalDate

/** A row Apparate could not read: its line in the file (1 is the header) and why. */
data class ImportProblem(val line: Int, val reason: String)

/** What a CSV would add: the new expenses, how many rows are already in the ledger, and the rows it could not read. */
data class ImportResult(
    val added: List<Expense>,
    val duplicates: Int,
    val problems: List<ImportProblem>,
    val rows: Int,
)

/**
 * Apparate: reads a CSV (the one Geminio exports, or a spreadsheet with the same columns, separated by
 * commas or semicolons) against the expenses already in the ledger. Nothing is changed here.
 * Rows match existing expenses one for one (same date, title, amount, category, food type and split),
 * so re-importing a backup adds nothing, and two identical coffees stay two coffees.
 */
object CsvImporter {
    private val required = listOf("date", "title", "category", "amount_eur")

    fun import(text: String, existing: List<Expense>, newId: () -> String): ImportResult {
        val clean = text.removePrefix("\uFEFF")
        val firstLine = clean.lineSequence().firstOrNull().orEmpty()
        val delimiter = if (';' in firstLine && ',' !in firstLine) ';' else ','
        val rows = parse(clean, delimiter)
        if (rows.isEmpty()) return ImportResult(emptyList(), 0, listOf(ImportProblem(1, "The file is empty.")), 0)

        val head = rows[0].map(::squash)
        fun col(name: String) = head.indexOf(squash(name))
        val missing = required.filter { col(it) < 0 }
        if (missing.isNotEmpty()) {
            return ImportResult(emptyList(), 0, listOf(ImportProblem(1, "Not an Expensiarmus CSV: no ${missing.joinToString(", ")} column.")), 0)
        }
        val at = mapOf(
            "date" to col("date"), "title" to col("title"), "category" to col("category"),
            "food" to col("food_type"), "amount" to col("amount_eur"), "percent" to col("my_share_percent"),
        )

        val left = existing.groupingBy(::sameness).eachCount().toMutableMap()
        val added = mutableListOf<Expense>()
        val problems = mutableListOf<ImportProblem>()
        var duplicates = 0
        var count = 0
        rows.drop(1).forEachIndexed { i, cells ->
            val line = i + 2
            if (cells.all { it.isBlank() }) return@forEachIndexed
            count++
            fun get(key: String) = at.getValue(key).let { if (it >= 0) cells.getOrNull(it).orEmpty().trim() else "" }
            fun problem(reason: String) { problems += ImportProblem(line, reason) }

            val date = parseDate(get("date")) ?: return@forEachIndexed problem("\"${get("date")}\" is not a date.")
            val title = get("title").ifEmpty { return@forEachIndexed problem("The title is empty.") }
            val category = findCategory(get("category")) ?: return@forEachIndexed problem("\"${get("category")}\" is not a category.")
            val foodText = get("food")
            val food = if (foodText.isEmpty()) null else findFood(foodText) ?: return@forEachIndexed problem("\"$foodText\" is not a food type.")
            if (food != null && category != Category.ENTERTAINMENT) return@forEachIndexed problem("Only Entertainment can have a food type.")
            val cents = Money.parseToCents(get("amount")) ?: return@forEachIndexed problem("\"${get("amount")}\" is not an amount.")
            val percentText = get("percent")
            val percent = if (percentText.isEmpty()) 100 else percentText.toIntOrNull()
            if (percent == null || percent !in 1..100) return@forEachIndexed problem("\"$percentText\" is not a share between 1 and 100.")

            val expense = Expense(newId(), date, title, cents, category, percent, food)
            val key = sameness(expense)
            val already = left[key] ?: 0
            if (already > 0) {
                left[key] = already - 1
                duplicates++
            } else {
                added += expense
            }
        }
        return ImportResult(added, duplicates, problems, count)
    }

    /** What the confirmation shows: counts, then up to [maxProblems] unreadable rows ("Line 4: …"). */
    fun summary(r: ImportResult, maxProblems: Int = 5): List<String> = buildList {
        add(if (r.rows == 1) "Found 1 expense in the file." else "Found ${r.rows} expenses in the file.")
        if (r.rows > 0) add("${r.added.size} new, ${r.duplicates} already in your ledger.")
        if (r.problems.isNotEmpty()) {
            add(if (r.problems.size == 1) "1 row could not be read:" else "${r.problems.size} rows could not be read:")
            r.problems.take(maxProblems).forEach { add("Line ${it.line}: ${it.reason}") }
            if (r.problems.size > maxProblems) add("…and ${r.problems.size - maxProblems} more.")
        }
    }

    /** Splits CSV text into rows of fields: quoted fields, doubled quotes and CRLF are handled. */
    fun parse(text: String, delimiter: Char = ','): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && text.getOrNull(i + 1) == '"' -> { field.append('"'); i++ }
                quoted && c == '"' -> quoted = false
                quoted -> field.append(c)
                c == '"' && field.isEmpty() -> quoted = true
                c == delimiter -> { row += field.toString(); field.clear() }
                c == '\n' || c == '\r' -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    row += field.toString(); field.clear()
                    rows += row; row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row += field.toString(); rows += row }
        return rows
    }

    /** "2026-11-22", or a spreadsheet's "22/11/2026" or "22.11.2026" (day first); null if not a real date. */
    fun parseDate(value: String): LocalDate? {
        val s = value.trim()
        val iso = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})$""").find(s)
        val dayFirst = Regex("""^(\d{1,2})[/.](\d{1,2})[/.](\d{4})$""").find(s)
        val (y, m, d) = when {
            iso != null -> iso.destructured.let { (y, m, d) -> Triple(y.toInt(), m.toInt(), d.toInt()) }
            dayFirst != null -> dayFirst.destructured.let { (d, m, y) -> Triple(y.toInt(), m.toInt(), d.toInt()) }
            else -> return null
        }
        return try { LocalDate.of(y, m, d) } catch (_: DateTimeException) { null }
    }

    private fun squash(v: String) = v.lowercase().filterNot { it.isWhitespace() || it == '_' || it == '-' }
    /** Earlier names still read, so old backups come back in: Home was called Rent before 1.12. */
    private val oldNames = mapOf("rent" to Category.RENT)
    private fun findCategory(v: String) =
        Category.entries.firstOrNull { squash(it.label) == squash(v) || squash(it.name) == squash(v) } ?: oldNames[squash(v)]
    private fun findFood(v: String) = FoodType.entries.firstOrNull { squash(it.label) == squash(v) || squash(it.name) == squash(v) }
    private fun sameness(e: Expense) = listOf(e.date, e.title.trim().lowercase(), e.amountCents, e.category, e.foodType, e.myPercent)
}
