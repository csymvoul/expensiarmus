package com.csymvoul.expenses.domain

/** How amounts are shown: €1,234.56 or, in the Greek style, 1.234,56 €. */
enum class MoneyStyle { ENGLISH, GREEK }

/** Euro amounts are kept as whole cents (Long) to avoid floating point rounding errors. */
object Money {
    /** The style chosen in Settings; used whenever no style is passed. */
    @Volatile var style: MoneyStyle = MoneyStyle.ENGLISH

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
    fun format(cents: Long, style: MoneyStyle = this.style): String {
        val sign = if (cents < 0) "-" else ""
        val abs = kotlin.math.abs(cents)
        val group = if (style == MoneyStyle.GREEK) "." else ","
        val euros = (abs / 100).toString().reversed().chunked(3).joinToString(group).reversed()
        val dec = (abs % 100).toString().padStart(2, '0')
        return if (style == MoneyStyle.GREEK) "$sign$euros,$dec €" else "$sign€$euros.$dec"
    }

    /** "1234.56", for CSV files. */
    fun plain(cents: Long): String {
        val sign = if (cents < 0) "-" else ""
        val abs = kotlin.math.abs(cents)
        return "$sign${abs / 100}.${(abs % 100).toString().padStart(2, '0')}"
    }

    /** Short form for calendar cells: "€45", "€1.2k", "<€1". Whole euros are rounded half up. */
    fun compact(cents: Long, style: MoneyStyle = this.style): String {
        val greek = style == MoneyStyle.GREEK
        fun wrap(n: String) = if (greek) "$n€" else "€$n"
        if (cents in 1..49) return "<${wrap("1")}"
        val euros = (cents + 50) / 100
        return when {
            euros < 1_000 -> wrap("$euros")
            euros < 10_000 -> wrap("${euros / 1_000}${if (greek) "," else "."}${(euros % 1_000) / 100}k")
            else -> wrap("${euros / 1_000}k")
        }
    }

    /** Text to pre-fill an amount field: "1234" for whole euros, otherwise "1234.56". */
    fun forInput(cents: Long, style: MoneyStyle = this.style): String = when {
        cents % 100 == 0L -> (cents / 100).toString()
        style == MoneyStyle.GREEK -> plain(cents).replace('.', ',')
        else -> plain(cents)
    }

    /** The decimal separator to offer on a number pad. */
    fun decimalKey(style: MoneyStyle = this.style): String = if (style == MoneyStyle.GREEK) "," else "."


    private const val MAX_EUROS = 100_000_000_000L
}
