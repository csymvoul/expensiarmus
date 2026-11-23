package com.csymvoul.expenses.ui

import com.csymvoul.expenses.domain.Words
import org.junit.Assert.assertEquals
import org.junit.Test

class WardButtonTest {
    @Test fun `cast a new ward loses its emoji for the painted shield`() {
        assertEquals("Cast a new ward", Words.withoutEmoji(Words.WIZARD.newWard))
        assertEquals("Add a goal", Words.withoutEmoji(Words.PLAIN.newWard))
    }
}
