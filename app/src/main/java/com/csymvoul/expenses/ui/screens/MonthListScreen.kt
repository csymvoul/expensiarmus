package com.csymvoul.expenses.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.csymvoul.expenses.domain.CategoryLine
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.ui.components.EmblemBadge
import com.csymvoul.expenses.ui.theme.color
import com.csymvoul.expenses.ui.theme.icon
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
 * A month's full list: a folding By vault card, then the days, each closed until tapped, with or
 * without the expenses of trips (the chart is on the Ledger). [openDays] is kept by the caller so it survives a visit to an expense.
 */
@Composable
fun MonthListScreen(
    month: YearMonth,
    data: AppData,
    openDays: Set<LocalDate>,
    onToggleDay: (LocalDate) -> Unit,
    catsOpen: Boolean,
    onToggleCats: () -> Unit,
    /** A category (or food type) for this month; true: with trips left out. */
    onCategory: (Category, FoodType?, Boolean) -> Unit,
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
    val lines = remember(shown) { ExpenseQueries.categoryTotals(shown) }
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
            if (lines.isNotEmpty()) {
                item(key = "by-vault") { ByVaultCard(lines, catsOpen, onToggleCats) { c, t -> onCategory(c, t, leaveOut) } }
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

/** The full list's By vault card: a line per category, Entertainment's food types under it; the heading folds it. */
@Composable
private fun ByVaultCard(lines: List<CategoryLine>, open: Boolean, onToggle: () -> Unit, onCategory: (Category, FoodType?) -> Unit) {
    val w = LocalWords.current
    val turn by animateFloatAsState(if (open) 90f else 0f, label = "chevron")
    fun pct(x: Float) = "${Math.round(x * 100)}%"
    ParchmentCard(spacing = Arrangement.spacedBy(0.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(bottom = if (open) 10.dp else 0.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(w.byVault, style = MaterialTheme.typography.titleMedium)
                if (!open) {
                    val top = lines.first()
                    Text(
                        "${top.category.label} ${pct(top.share)} · " + if (lines.size == 1) "1 ${w.vault}" else "${lines.size} ${w.vaults}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = if (open) "Fold" else "Open", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.rotate(turn))
        }
        if (!open) return@ParchmentCard
        // A thin bar in the categories' colours.
        Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).padding(bottom = 0.dp)) {
            lines.forEach { l -> Box(Modifier.weight(l.cents.toFloat()).fillMaxHeight().background(l.category.color)) }
        }
        Spacer(Modifier.height(6.dp))
        lines.forEachIndexed { i, l ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().clickable { onCategory(l.category, null) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                EmblemBadge(l.category.icon, size = 24.dp)
                Spacer(Modifier.width(8.dp))
                Text(l.category.label, modifier = Modifier.weight(1f))
                Text(Money.format(l.cents), fontWeight = FontWeight.SemiBold)
                Text(pct(l.share), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            l.food.forEach { (t, cents) ->
                Row(Modifier.fillMaxWidth().clickable { onCategory(l.category, t) }.padding(start = 32.dp, top = 2.dp, bottom = 2.dp, end = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    EmblemBadge(t.icon, size = 18.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(t.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(Money.format(cents), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (l.otherCents > 0) {
                Row(Modifier.fillMaxWidth().padding(start = 32.dp, top = 2.dp, bottom = 2.dp, end = 56.dp)) {
                    Text("Other entertainment", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(Money.format(l.otherCents), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
