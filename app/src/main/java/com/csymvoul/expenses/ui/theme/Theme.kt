package com.csymvoul.expenses.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.csymvoul.expenses.R
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.FoodType

/** Engraved capitals for headings (Cinzel, OFL licensed; see licenses/). */
val Cinzel = FontFamily(
    Font(R.font.cinzel, FontWeight.Normal),
    Font(R.font.cinzel, FontWeight.SemiBold),
    Font(R.font.cinzel, FontWeight.Bold),
)

// Light: parchment, burgundy ink and gold leaf.
private val Parchment = lightColorScheme(
    primary = Color(0xFF7B1E1E),
    onPrimary = Color(0xFFFFF4DC),
    primaryContainer = Color(0xFFEBC9A6),
    onPrimaryContainer = Color(0xFF3B0A0A),
    secondary = Color(0xFFA87A1E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1DDA6),
    onSecondaryContainer = Color(0xFF3A2A00),
    tertiary = Color(0xFF2E5A43),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF3E6C8),
    onBackground = Color(0xFF3B2A1A),
    surface = Color(0xFFF3E6C8),
    onSurface = Color(0xFF3B2A1A),
    surfaceVariant = Color(0xFFE6D3AA),
    onSurfaceVariant = Color(0xFF6B5236),
    surfaceContainerLowest = Color(0xFFFFF8E8),
    surfaceContainerLow = Color(0xFFF8EDD3),
    surfaceContainer = Color(0xFFF1E2BF),
    surfaceContainerHigh = Color(0xFFECDAB3),
    surfaceContainerHighest = Color(0xFFE8D3A6),
    outline = Color(0xFFA8875A),
    outlineVariant = Color(0xFFD2B98A),
    error = Color(0xFFA31515),
    onError = Color(0xFFFFFFFF),
)

// Dark: a candle-lit castle at night, gold on deep brown.
private val Night = darkColorScheme(
    primary = Color(0xFFE0B656),
    onPrimary = Color(0xFF3A2800),
    primaryContainer = Color(0xFF6B1A1A),
    onPrimaryContainer = Color(0xFFFFDCC8),
    secondary = Color(0xFFD9A441),
    onSecondary = Color(0xFF3A2800),
    secondaryContainer = Color(0xFF5A3E12),
    onSecondaryContainer = Color(0xFFF8E2B0),
    tertiary = Color(0xFF8FC9A6),
    onTertiary = Color(0xFF0F2E1E),
    background = Color(0xFF17120E),
    onBackground = Color(0xFFEDE0C4),
    surface = Color(0xFF17120E),
    onSurface = Color(0xFFEDE0C4),
    surfaceVariant = Color(0xFF3A2E22),
    onSurfaceVariant = Color(0xFFCDBB98),
    surfaceContainerLowest = Color(0xFF120E0A),
    surfaceContainerLow = Color(0xFF1F1813),
    surfaceContainer = Color(0xFF241C16),
    surfaceContainerHigh = Color(0xFF2E241C),
    surfaceContainerHighest = Color(0xFF3A2E24),
    outline = Color(0xFF8F7A5A),
    outlineVariant = Color(0xFF4D3E2E),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5C0000),
)

private fun TextStyle.heading() = copy(fontFamily = Cinzel, fontWeight = FontWeight.SemiBold)
private fun TextStyle.body() = copy(fontFamily = FontFamily.Serif)

private val base = Typography()
private val WizardTypography = Typography(
    displayLarge = base.displayLarge.heading(),
    displayMedium = base.displayMedium.heading(),
    displaySmall = base.displaySmall.heading(),
    headlineLarge = base.headlineLarge.heading().copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.heading(),
    headlineSmall = base.headlineSmall.heading(),
    titleLarge = base.titleLarge.heading().copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    titleMedium = base.titleMedium.heading(),
    titleSmall = base.titleSmall.heading(),
    bodyLarge = base.bodyLarge.body(),
    bodyMedium = base.bodyMedium.body(),
    bodySmall = base.bodySmall.body(),
    labelLarge = base.labelLarge.body(),
    labelMedium = base.labelMedium.body(),
    labelSmall = base.labelSmall.body(),
)

/** Parchment by day, castle by night: follows the phone's light/dark setting. */
@Composable
fun ExpensesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Night else Parchment,
        typography = WizardTypography,
        content = content,
    )
}

/** Gem-like colours, readable on parchment and on the dark theme. */
val Category.color: Color
    get() = when (this) {
        Category.RENT -> Color(0xFFA33A3A) // ruby
        Category.SUPERMARKET -> Color(0xFF2F7D4E) // emerald
        Category.ENTERTAINMENT -> Color(0xFFC8962E) // gold
        Category.BILLS -> Color(0xFF3A63A8) // sapphire
        Category.SHOPPING -> Color(0xFFB5487A) // rose quartz
        Category.OTHER -> Color(0xFF7E5AA6) // amethyst
    }

/** Shades of the Entertainment gold. */
val FoodType.color: Color
    get() = when (this) {
        FoodType.DELIVERY -> Color(0xFF8C6416)
        FoodType.DINE_OUT -> Color(0xFFC8962E)
        FoodType.DAY_TO_DAY -> Color(0xFFE6C878)
    }

/** Amber used once 80% of a goal is spent. */
val WarningColor = Color(0xFFE08A00)
