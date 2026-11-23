package com.csymvoul.expenses.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.csymvoul.expenses.domain.Words

/** The wording chosen in Settings (wizard or plain), available to every screen. */
val LocalWords = staticCompositionLocalOf { Words.WIZARD }

enum class Tab { LEDGER, CALENDAR, ANNALS, GOALS }

/** Bottom tab bar on a parchment band: Ledger · Calendar · Annals · Protego (or the plain names). */
@Composable
fun WizardTabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val w = LocalWords.current
    val labels = mapOf(Tab.LEDGER to w.ledgerTab, Tab.CALENDAR to w.calendarTab, Tab.ANNALS to w.annalsTab, Tab.GOALS to w.goalsTab)
    Column {
        GoldRule()
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Tab.entries.forEach { tab ->
                val (icon, name) = labels.getValue(tab).split(" ", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
                NavigationBarItem(
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    icon = { Text(icon, fontSize = 20.sp, modifier = Modifier.alpha(if (tab == selected) 1f else 0.7f)) },
                    label = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                    ),
                )
            }
        }
    }
}
