package com.csymvoul.expenses.ui

import com.csymvoul.expenses.ui.components.GoldRuleLines
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoldRuleTest {
    @Test fun `on the tab bar the gold line sits on the top edge`() {
        val tab = GoldRuleLines.of(atTopEdge = true)
        assertTrue("bright line within 1dp of the edge", tab.firstDp <= 1f)
        assertTrue(tab.secondDp < tab.heightDp)
        assertEquals(3f, tab.secondDp - tab.firstDp, 0.001f) // same double line as everywhere else
    }

    @Test fun `under the top bar the rule keeps its usual shape`() {
        assertEquals(GoldRuleLines(10f, 3f, 6f), GoldRuleLines.of(atTopEdge = false))
    }
}
