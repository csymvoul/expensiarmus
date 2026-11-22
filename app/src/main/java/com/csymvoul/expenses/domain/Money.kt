package com.csymvoul.expenses.domain

/** Euro amounts are kept as whole cents (Long) to avoid floating point rounding errors. */
object Money {
    private val allowed = Regex("[0-9.,]+")

    /**
     * Parses what the user typed into cents. Accepts a comma or a dot as the decimal
     * separator ("12,50", "12.5") and ignores thousands separators ("1.234,56", "1,234.56").
     * A separator followed by exactly three digits is read as a thousands separator.
     * Returns null when the text is not an amount.
     */
    fun parseToCents(input: String): Long? {
        val s = input.trim().removePrefix("€").removeSuffix("€").trim()
        if (!allowed.matches(s)) return null
        val lastSep = maxOf(s.lastIndexOf('.'), s.lastIndexOf(','))
        val decimals = if (lastSep >= 0) s.length - lastSep - 1 else 0
        val intRaw: String
        val fracRaw: String
        when {
            lastSep >= 0 && decimals in 1..2 -> {
                intRaw = s.substring(0, lastSep)
                fracRaw = s.substring(lastSep + 1)
            }
            lastSep >= 0 && decimals == 0 -> return null
            else -> {
                intRaw = s
                fracRaw = ""
            }
        }
        val intDigits = intRaw.filter { it.isDigit() }
        if (intDigits.isEmpty() && fracRaw.isEmpty()) return null
        val euros = intDigits.ifEmpty { "0" }.toLongOrNull() ?: return null
        if (euros > MAX_EUROS) return null
        val cents = fracRaw.padEnd(2, '0').toLong()
        return euros * 100 + cents
    }

    /** "€1,234.56" */
    fun format(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = kotlin.math.abs(cents)
        val euros = (abs / 100).toString().reversed().chunked(3).joinToString(",").reversed()
        return "$sign€$euros.${(abs % 100).toString().padStart(2, '0')}"
    }

    /** "1234.56", for CSV files. */
    fun plain(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = kotlin.math.abs(cents)
        return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    /** Text to pre-fill an amount field: "1234" for whole euros, otherwise "1234.56". */
    fun forInput(cents: Long): String = if (cents % 100 == 0L) (cents / 100).toString() else plain(cents)

    private const val MAX_EUROS = 100_000_000_000L
}
