package com.csymvoul.expenses.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class GoalsTest {
    private val oct = YearMonth.of(2026, 10)
    private val nov = YearMonth.of(2026, 11)
    private val dec = YearMonth.of(2026, 12)
    private val jan = YearMonth.of(2027, 1)
    private val ent = Category.ENTERTAINMENT
    private fun Goals.cents(c: Category, m: YearMonth) = goalFor(c, m)?.cents

    @Test fun `no goals`() = assertNull(Goals().goalFor(ent, nov))

    @Test fun `this month only`() {
        val g = Goals().set(ent, nov, 6_000, onwards = false)
        assertEquals(6_000L, g.cents(ent, nov))
        assertNull(g.cents(ent, oct))
        assertNull(g.cents(ent, dec))
        assertNull(g.cents(Category.FUEL, nov))
    }

    @Test fun `this month onwards covers later months, not earlier ones`() {
        val g = Goals().set(ent, nov, 6_000, onwards = true)
        assertNull(g.cents(ent, oct))
        assertEquals(6_000L, g.cents(ent, nov))
        assertEquals(6_000L, g.cents(ent, jan))
    }

    @Test fun `a single-month goal wins over an onwards goal`() {
        val g = Goals().set(ent, nov, 6_000, onwards = true).set(ent, dec, 9_000, onwards = false)
        assertEquals(6_000L, g.cents(ent, nov))
        assertEquals(9_000L, g.cents(ent, dec))
        assertEquals(6_000L, g.cents(ent, jan))
    }

    @Test fun `a later onwards goal takes over from its month`() {
        val g = Goals().set(ent, oct, 5_000, onwards = true).set(ent, dec, 7_000, onwards = true)
        assertEquals(5_000L, g.cents(ent, nov))
        assertEquals(7_000L, g.cents(ent, dec))
        assertEquals(7_000L, g.cents(ent, jan))
    }

    @Test fun `setting onwards replaces later onwards goals and this month's own goal`() {
        val g = Goals()
            .set(ent, dec, 7_000, onwards = true)
            .set(ent, nov, 4_000, onwards = false)
            .set(ent, nov, 6_000, onwards = true)
        assertEquals(6_000L, g.cents(ent, nov))
        assertEquals(6_000L, g.cents(ent, dec))
        assertEquals(1, g.entries.size)
    }

    @Test fun `changing an amount for this month only`() {
        val g = Goals().set(ent, oct, 5_000, onwards = true).set(ent, nov, 8_000, onwards = false)
        assertEquals(5_000L, g.cents(ent, oct))
        assertEquals(8_000L, g.cents(ent, nov))
        assertEquals(5_000L, g.cents(ent, dec))
    }

    @Test fun `remove for this month only keeps other months`() {
        val g = Goals().set(ent, oct, 5_000, onwards = true).remove(ent, nov, onwards = false)
        assertEquals(5_000L, g.cents(ent, oct))
        assertNull(g.cents(ent, nov))
        assertEquals(5_000L, g.cents(ent, dec))
    }

    @Test fun `remove from this month onwards keeps earlier months`() {
        val g = Goals()
            .set(ent, oct, 5_000, onwards = true)
            .set(ent, jan, 9_000, onwards = false)
            .remove(ent, nov, onwards = true)
        assertEquals(5_000L, g.cents(ent, oct))
        assertNull(g.cents(ent, nov))
        assertNull(g.cents(ent, dec))
        assertNull(g.cents(ent, jan))
    }

    @Test fun `removing a single-month goal without anything underneath leaves no entries`() {
        val g = Goals().set(ent, nov, 6_000, onwards = false).remove(ent, nov, onwards = false)
        assertNull(g.cents(ent, nov))
        assertEquals(0, g.entries.size)
    }

    @Test fun `old goals convert to an every-month entertainment goal`() {
        val old = Goals(monthlyCents = 120_000, perCategoryCents = mapOf(ent to 6_000, Category.RENT to 50_000))
        val g = old.migrated()
        assertEquals(6_000L, g.cents(ent, YearMonth.of(2024, 3)))
        assertEquals(6_000L, g.cents(ent, jan))
        assertNull(g.cents(Category.RENT, nov))
        assertNull(g.monthlyCents)
        assertEquals(emptyMap<Category, Long>(), g.perCategoryCents)
    }

    @Test fun `new-format goals are left alone by migration`() {
        val g = Goals().set(ent, nov, 6_000, onwards = false)
        assertEquals(g, g.migrated())
    }
}
