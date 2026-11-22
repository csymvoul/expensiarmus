package com.csymvoul.expenses.ui

import com.csymvoul.expenses.ui.components.DonutMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DonutMathTest {
    // 200 x 200 chart, ring 20 wide: ring centre line has radius 90.
    private fun tap(values: List<Float>, x: Float, y: Float) = DonutMath.sliceAt(values, x, y, 200f, 200f, 20f)

    private val quarters = listOf(1f, 1f, 1f, 1f) // each slice is 90 degrees, clockwise from 12 o'clock

    @Test fun `taps on the ring pick the slice clockwise from the top`() {
        assertEquals(0, tap(quarters, 140f, 30f))  // upper right
        assertEquals(1, tap(quarters, 160f, 150f)) // lower right
        assertEquals(2, tap(quarters, 50f, 160f))  // lower left
        assertEquals(3, tap(quarters, 40f, 50f))   // upper left
    }

    @Test fun `tap in the hole or outside the ring picks nothing`() {
        assertNull(tap(quarters, 100f, 100f))
        assertNull(tap(quarters, 100f, 60f))
        assertNull(tap(quarters, 2f, 2f))
    }

    @Test fun `zero slices are skipped`() {
        // Only indexes 1 and 3 have spending: right half is 1, left half is 3.
        val values = listOf(0f, 5f, 0f, 5f)
        assertEquals(1, tap(values, 190f, 100f))
        assertEquals(3, tap(values, 10f, 100f))
    }

    @Test fun `a single slice takes the whole ring`() {
        assertEquals(2, tap(listOf(0f, 0f, 7f), 100f, 190f))
    }

    @Test fun `empty chart picks nothing`() {
        assertNull(tap(listOf(0f, 0f), 190f, 100f))
        assertNull(tap(emptyList(), 190f, 100f))
    }

    @Test fun `small slice is still hittable`() {
        // 10% slice from 0 to 36 degrees, then 90%.
        val values = listOf(1f, 9f)
        assertEquals(0, tap(values, 120f, 12f)) // about 13 degrees from the top
        assertEquals(1, tap(values, 190f, 100f))
    }
}
