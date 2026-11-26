package com.csymvoul.expenses.ui.screens

import com.csymvoul.expenses.ui.components.EmblemBadge
import com.csymvoul.expenses.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import com.csymvoul.expenses.ui.components.ChipEmblem
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.items
import com.csymvoul.expenses.ui.components.WizardButton
import com.csymvoul.expenses.domain.TripSummary
import com.csymvoul.expenses.ui.components.Tab
import com.csymvoul.expenses.ui.components.TodayGlyph
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.domain.PeriodSummary
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.ui.components.BarChart
import com.csymvoul.expenses.ui.components.LocalWords
import com.csymvoul.expenses.ui.components.ParchmentCard
import com.csymvoul.expenses.ui.components.WizardTopBar
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AnnalsView { YEAR, ALL_TIME, TRIPS }

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val shortMonth = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

/** Totals for a whole year, or for everything ever recorded. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnalsScreen(
    summary: PeriodSummary,
    view: AnnalsView,
    year: Int,
    today: LocalDate,
    onViewChange: (AnnalsView) -> Unit,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    /** Jumps back to the current year (By year only). */
    onThisYear: () -> Unit,
    onOpenMonth: (YearMonth) -> Unit,
    onOpenYear: (Int) -> Unit,
    onCategory: (Category, FoodType?) -> Unit,
    /** Annals → Trips: every trip, newest first. */
    trips: List<TripSummary>,
    onOpenTrip: (String) -> Unit,
    onEditTrips: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val w = LocalWords.current
    // The tapped bar, reset when switching year or view.
    var selected by rememberSaveable(view, year) { mutableStateOf<Int?>(null) }
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            WizardTopBar(
                title = w.annals,
                icon = Tab.ANNALS.icon,
                actions = {
                    // Only on By year, away from the current year: the Time-Turner brings you back to it.
                    if (view == AnnalsView.YEAR && year != today.year) {
                        IconButton(onClick = onThisYear) { TodayGlyph(w.thisMonth(today.year.toString())) }
                    }
                },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    AnnalsView.entries.forEachIndexed { i, v ->
                        SegmentedButton(
                            selected = view == v,
                            onClick = { onViewChange(v) },
                            shape = SegmentedButtonDefaults.itemShape(i, AnnalsView.entries.size),
                            icon = {
                                when (v) {
                                    AnnalsView.YEAR -> ChipEmblem(Tab.CALENDAR.icon)
                                    AnnalsView.ALL_TIME -> ChipEmblem(Tab.ANNALS.icon)
                                    AnnalsView.TRIPS -> ChipEmblem(R.drawable.emblem_trip)
                                }
                            },
                        ) {
                            Text(
                                when (v) {
                                    AnnalsView.YEAR -> "By year"
                                    AnnalsView.ALL_TIME -> "All time"
                                    AnnalsView.TRIPS -> w.trips
                                },
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            if (view == AnnalsView.TRIPS) {
                if (trips.isEmpty()) {
                    item {
                        Text("No ${w.trips.lowercase()} yet. Add one in Settings → ${w.trips}.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
                    }
                }
                items(trips, key = { it.trip.id }) { s -> TripCard(s, onClick = { onOpenTrip(s.trip.id) }) }
                item {
                    WizardButton(onClick = onEditTrips, modifier = Modifier.fillMaxWidth()) {
                        EmblemBadge(R.drawable.emblem_trip, size = 24.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(if (trips.isEmpty()) w.newTrip else "Edit ${w.trips.lowercase()}")
                    }
                }
                return@LazyColumn
            }
            if (view == AnnalsView.YEAR) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPreviousYear) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous year",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            year.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                        )
                        IconButton(onClick = onNextYear) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next year",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            item { PeriodTotalCard(summary, view, year, today) }
            item {
                ParchmentCard(spacing = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (view == AnnalsView.YEAR) "Month by month" else "Year by year",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (summary.bars.isEmpty()) {
                        Text("Nothing recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        BarChart(
                            values = summary.bars.map { it.cents },
                            labels = summary.bars.map { it.label },
                            selected = selected,
                            onSelect = { selected = if (selected == it) null else it },
                        )
                        val bar = selected?.let { summary.bars.getOrNull(it) }
                        if (bar == null) {
                            Text(
                                "Tap a bar to see its total.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${bar.month?.format(monthFormat) ?: bar.year}: ${Money.format(bar.cents)}",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f),
                                )
                                TextButton(onClick = {
                                    bar.month?.let(onOpenMonth) ?: bar.year?.let(onOpenYear)
                                }) { Text(if (bar.month != null) "Open month ›" else "Open year ›") }
                            }
                        }
                    }
                }
            }
            item {
                BreakdownCard(summary.categories, summary.totalCents, "$view-$year", onCategory, period = if (view == AnnalsView.YEAR) year.toString() else "")
            }
            if (summary.food.totalCents > 0) {
                item { FoodCard(summary.food) { onCategory(Category.ENTERTAINMENT, it) } }
            }
        }
    }
}

@Composable
private fun PeriodTotalCard(summary: PeriodSummary, view: AnnalsView, year: Int, today: LocalDate) {
    ParchmentCard(spacing = Arrangement.spacedBy(4.dp)) {
        Text(
            if (view == AnnalsView.YEAR) LocalWords.current.spentIn(year) else LocalWords.current.spentAllTime,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            Money.format(summary.totalCents),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (summary.paidTotalCents != summary.totalCents) {
            Text(
                "Your share. Full bills came to ${Money.format(summary.paidTotalCents)}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (summary.monthsCounted > 0) {
            Text(
                "Monthly average: ${Money.format(summary.averagePerMonthCents)} " +
                    "over ${if (summary.monthsCounted == 1) "1 month" else "${summary.monthsCounted} months"}",
            )
        }
        summary.changePercent?.let { change ->
            val span = if (year == today.year) {
                "Jan–${today.format(shortMonth)} ${year - 1}"
            } else {
                "${year - 1}"
            }
            Text(
                when {
                    change > 0 -> "▲ $change% more than $span"
                    change < 0 -> "▼ ${-change}% less than $span"
                    else -> "Same as $span"
                },
                color = if (change > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
            )
        }
        Text(
            buildString {
                append(if (summary.expenseCount == 1) "1 expense" else "${summary.expenseCount} expenses")
                summary.biggest?.let { append(" · biggest: ${it.title}, ${Money.format(it.myShareCents)}") }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
