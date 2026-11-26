package com.csymvoul.expenses.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import com.csymvoul.expenses.domain.DayGroup
import com.csymvoul.expenses.model.AppData
import java.time.LocalDate
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

/**
 * A month's full list: the days, each closed until tapped, with or without the expenses of trips
 * (the categories are on the Ledger). [openDays] is kept by the caller so it survives a visit to an expense.
 */
@Composable
fun MonthListScreen(
    month: YearMonth,
    data: AppData,
    openDays: Set<LocalDate>,
    onToggleDay: (LocalDate) -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    val w = LocalWords.current
    var withTrips by rememberSaveable(month) { mutableStateOf(true) }
    val expenses = remember(data, month) { data.expenses.filter { YearMonth.from(it.date) == month } }
    val without = remember(expenses) { Trips.withoutTrips(expenses) }
    val hasTrips = without.size != expenses.size
    val leaveOut = hasTrips && !withTrips
    val shown = if (leaveOut) without else expenses
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
                        (if (shown.size == 1) "1 expense" else "${shown.size} expenses") + if (leaveOut) ", without ${w.trips.lowercase()}" else "",
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
                val open = d.date in openDays
                item(key = "day-${d.date}") { DayHeader(d, open, onClick = { onToggleDay(d.date) }) }
                if (open) {
                    items(d.expenses, key = { it.id }) { e ->
                        ExpenseRow(e, onClick = { onOpen(e.id) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

/** A day on the full list: date, how many, total and a chevron that turns down when open. */
@Composable
private fun DayHeader(d: DayGroup, open: Boolean, onClick: () -> Unit) {
    val turn by animateFloatAsState(if (open) 90f else 0f, label = "chevron")
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(top = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(d.date.format(dayTitle), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(
                if (d.expenses.size == 1) "1 expense" else "${d.expenses.size} expenses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(Money.format(d.totalCents), style = MaterialTheme.typography.titleSmall)
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = if (open) "Close" else "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(turn),
            )
        }
        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f))
    }
}
