package com.csymvoul.expenses.ui

import androidx.compose.ui.graphics.Color
import com.csymvoul.expenses.ui.theme.wizardTypography
import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyTest {
    private val gold = Color(0xFFE0B656)
    private val t = wizardTypography(heading = gold)

    @Test fun `titles and headlines use the heading colour`() {
        listOf(t.headlineLarge, t.headlineSmall, t.titleLarge, t.titleMedium, t.titleSmall, t.displaySmall)
            .forEach { assertEquals(gold, it.color) }
    }

    @Test fun `body and label text keep the surrounding content colour`() {
        listOf(t.bodyLarge, t.bodyMedium, t.bodySmall, t.labelLarge, t.labelMedium, t.labelSmall)
            .forEach { assertEquals(Color.Unspecified, it.color) }
    }
}
