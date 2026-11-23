package com.csymvoul.expenses.ui

import com.csymvoul.expenses.domain.Words
import com.csymvoul.expenses.ui.components.ruleEndSparks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEndsTest {
    @Test fun `each end has a big star, a small star and two specks`() {
        val sparks = ruleEndSparks(400f)
        assertEquals(8, sparks.size)
        val left = sparks.filter { it.x < 200f }
        assertEquals(2, left.count { !it.speck })
        assertEquals(2, left.count { it.speck })
        assertEquals(4.5f, left.maxOf { it.radius })
    }

    @Test fun `the right end mirrors the left`() {
        val sparks = ruleEndSparks(400f)
        val (left, right) = sparks.partition { it.x < 200f }
        assertEquals(left.map { 400f - it.x }, right.map { it.x })
        assertEquals(left.map { it.dy }, right.map { it.dy })
    }

    @Test fun `flipped clusters point the other way`() {
        val up = ruleEndSparks(400f)
        val down = ruleEndSparks(400f, flip = true)
        assertEquals(up.map { -it.dy }, down.map { it.dy })
        assertTrue(up.any { it.dy < 0 })
    }

    @Test fun `cast a new ward loses its emoji for the painted shield`() {
        assertEquals("Cast a new ward", Words.withoutEmoji(Words.WIZARD.newWard))
        assertEquals("Add a goal", Words.withoutEmoji(Words.PLAIN.newWard))
    }
}
