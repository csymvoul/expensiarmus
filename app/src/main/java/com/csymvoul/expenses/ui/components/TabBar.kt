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
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.csymvoul.expenses.R
import com.csymvoul.expenses.domain.Words

/** The wording chosen in Settings (wizard or plain), available to every screen. */
val LocalWords = staticCompositionLocalOf { Words.WIZARD }

enum class Tab(@DrawableRes val icon: Int) {
    LEDGER(R.drawable.emblem_tab_ledger),
    CALENDAR(R.drawable.emblem_tab_calendar),
    ANNALS(R.drawable.emblem_tab_annals),
    GOALS(R.drawable.emblem_tab_goals),
    SETTINGS(R.drawable.emblem_tab_settings),
}

/** Unselected tabs show their emblem faded and partly drained of colour. */
private val faded = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.4f) })

/** Bottom tab bar on a parchment band: Ledger · Calendar · Annals · Protego · Settings (or the plain names). */
@Composable
fun WizardTabBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val w = LocalWords.current
    val labels = mapOf(
        Tab.LEDGER to w.ledgerTab, Tab.CALENDAR to w.calendarTab, Tab.ANNALS to w.annalsTab,
        Tab.GOALS to w.goalsTab, Tab.SETTINGS to w.settingsTab,
    )
    // One opaque band: the gold rule sits right on the tab bar's top edge, with no see-through gap above the tabs.
    Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
        GoldRule(atTopEdge = true)
        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            Tab.entries.forEach { tab ->
                val name = labels.getValue(tab).split(" ", limit = 2).let { parts -> parts.getOrElse(1) { parts[0] } }
                val on = tab == selected
                NavigationBarItem(
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    icon = {
                        Image(
                            painterResource(tab.icon),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            colorFilter = if (on) null else faded,
                            modifier = Modifier
                                .size(28.dp)
                                .alpha(if (on) 1f else 0.75f)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(6.dp)),
                        )
                    },
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
