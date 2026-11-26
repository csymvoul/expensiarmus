package com.csymvoul.expenses.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.domain.ExpenseQueries
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.domain.Trips
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.ui.components.ExpenseRow
import com.csymvoul.expenses.ui.components.LocalWords
import com.csymvoul.expenses.ui.components.ParchmentCard
import com.csymvoul.expenses.ui.components.Tab
import com.csymvoul.expenses.ui.components.WizardTopBar
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthTitle = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val dayTitle = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

/** A month's full list, by day, with or without the expenses of trips. [expenses] are that month's. */
@Composable
fun MonthListScreen(month: YearMonth, expenses: List<Expense>, onOpen: (String) -> Unit, onBack: () -> Unit) {
    val w = LocalWords.current
    var withTrips by rememberSaveable(month) { mutableStateOf(true) }
    val without = remember(expenses) { Trips.withoutTrips(expenses) }
    val hasTrips = without.size != expenses.size
    val shown = if (hasTrips && !withTrips) without else expenses
    val days = remember(shown) { ExpenseQueries.byDay(shown) }
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            WizardTopBar(
                title = month.format(monthTitle),
                icon = Tab.LEDGER.icon,
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                ParchmentCard(spacing = Arrangement.spacedBy(4.dp)) {
                    Text(w.spentThisMonth, style = MaterialTheme.typography.labelLarge)
                    Text(Money.format(shown.sumOf { it.myShareCents }), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    Text(
                        (if (shown.size == 1) "1 expense" else "${shown.size} expenses") + if (hasTrips && !withTrips) ", without ${w.trips.lowercase()}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (hasTrips) {
                item {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(selected = withTrips, onClick = { withTrips = true }, shape = SegmentedButtonDefaults.itemShape(0, 2)) {
                            Text("With ${w.trips.lowercase()} · ${Money.format(expenses.sumOf { it.myShareCents })}", maxLines = 1)
                        }
                        SegmentedButton(selected = !withTrips, onClick = { withTrips = false }, shape = SegmentedButtonDefaults.itemShape(1, 2)) {
                            Text("Without · ${Money.format(without.sumOf { it.myShareCents })}", maxLines = 1)
                        }
                    }
                }
            }
            if (days.isEmpty()) {
                item { Text(w.ledgerEmpty, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp)) }
            }
            days.forEach { d ->
                item(key = "day-${d.date}") {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(d.date.format(dayTitle), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(Money.format(d.totalCents), style = MaterialTheme.typography.titleSmall)
                    }
                }
                items(d.expenses, key = { it.id }) { e ->
                    ExpenseRow(e, onClick = { onOpen(e.id) })
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}
