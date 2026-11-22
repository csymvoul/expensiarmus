package com.csymvoul.expenses.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test fun `parses whole euros`() = assertEquals(1_200L, Money.parseToCents("12"))
    @Test fun `parses dot decimals`() = assertEquals(1_250L, Money.parseToCents("12.50"))
    @Test fun `parses comma decimals`() = assertEquals(1_250L, Money.parseToCents("12,50"))
    @Test fun `parses one decimal digit`() = assertEquals(1_250L, Money.parseToCents("12.5"))
    @Test fun `parses leading separator`() = assertEquals(50L, Money.parseToCents(",50"))
    @Test fun `parses greek thousands format`() = assertEquals(123_456L, Money.parseToCents("1.234,56"))
    @Test fun `parses english thousands format`() = assertEquals(123_456L, Money.parseToCents("1,234.56"))
    @Test fun `three digits after separator are thousands`() = assertEquals(123_400L, Money.parseToCents("1.234"))
    @Test fun `ignores euro sign and spaces`() = assertEquals(990L, Money.parseToCents(" €9,90 "))

    @Test fun `rejects junk`() {
        assertNull(Money.parseToCents(""))
        assertNull(Money.parseToCents("abc"))
        assertNull(Money.parseToCents("12a"))
        assertNull(Money.parseToCents("-5"))
        assertNull(Money.parseToCents("12."))
        assertNull(Money.parseToCents("."))
        assertNull(Money.parseToCents("99999999999999999999"))
    }

    @Test fun `formats with grouping`() {
        assertEquals("€0.00", Money.format(0))
        assertEquals("€0.05", Money.format(5))
        assertEquals("€12.50", Money.format(1_250))
        assertEquals("€1,234.56", Money.format(123_456))
        assertEquals("€1,000,000.00", Money.format(100_000_000))
        assertEquals("-€3.10", Money.format(-310))
    }

    @Test fun `plain and input formats`() {
        assertEquals("1234.56", Money.plain(123_456))
        assertEquals("0.07", Money.plain(7))
        assertEquals("1200", Money.forInput(120_000))
        assertEquals("12.50", Money.forInput(1_250))
    }

    @Test fun `input format parses back to the same value`() {
        listOf(0L, 7L, 1_250L, 120_000L, 123_456L).forEach {
            assertEquals(it, Money.parseToCents(Money.forInput(it)))
        }
    }
}
