package com.csymvoul.expenses.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.FoodType

/** Follows the phone's light/dark setting, and its wallpaper colours on Android 12+. */
@Composable
fun ExpensesTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

val Category.color: Color
    get() = when (this) {
        Category.RENT -> Color(0xFF5C6BC0)
        Category.SUPERMARKET -> Color(0xFF43A047)
        Category.ENTERTAINMENT -> Color(0xFFFB8C00)
        Category.BILLS -> Color(0xFFE53935)
        Category.OTHER -> Color(0xFF8E24AA)
    }

/** Shades of the Entertainment orange. */
val FoodType.color: Color
    get() = when (this) {
        FoodType.DELIVERY -> Color(0xFFBF360C)
        FoodType.DINE_OUT -> Color(0xFFF57C00)
        FoodType.DAY_TO_DAY -> Color(0xFFFFB74D)
    }

/** Amber used once 80% of a goal is spent. */
val WarningColor = Color(0xFFFB8C00)
