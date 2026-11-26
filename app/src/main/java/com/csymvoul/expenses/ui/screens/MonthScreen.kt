package com.csymvoul.expenses.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csymvoul.expenses.domain.CategoryTotal
import com.csymvoul.expenses.domain.FoodSummary
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.domain.MonthlySummary
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.ui.components.CalendarGrid
import com.csymvoul.expenses.ui.components.EmblemBadge
import com.csymvoul.expenses.ui.components.TodayGlyph
import com.csymvoul.expenses.ui.components.SearchGlyph
import com.csymvoul.expenses.ui.components.WizardFab
import com.csymvoul.expenses.ui.components.WizardTopBar
import com.csymvoul.expenses.ui.components.DonutChart
import com.csymvoul.expenses.ui.components.ExpenseRow
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import java.time.format.TextStyle
import java.time.YearMonth
import com.csymvoul.expenses.domain.ExpenseQueries
import com.csymvoul.expenses.domain.MonthGlance
import com.csymvoul.expenses.domain.MonthRecap
import com.csymvoul.expenses.domain.TripSummary
import com.csymvoul.expenses.ui.components.GoalBar
import com.csymvoul.expenses.ui.components.LocalWords
import com.csymvoul.expenses.ui.components.OrnamentHeading
import com.csymvoul.expenses.ui.components.ParchmentCard
import com.csymvoul.expenses.ui.theme.color
import com.csymvoul.expenses.ui.theme.icon
import com.csymvoul.expenses.R
import com.csymvoul.expenses.ui.theme.nickname
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val longDayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)

enum class MonthView { OVERVIEW, CALENDAR }

/** What the calendar shows below the grid. */
data class DayDetails(
    val glance: MonthGlance,
    val totals: Map<LocalDate, Long>,
    val selected: LocalDate?,
    val expenses: List<Expense>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    summary: MonthlySummary,
    view: MonthView,
    days: DayDetails,
    onSelectDay: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    /** Jumps back to the current month. */
    onThisMonth: () -> Unit,
    /** Last month in one line, shown on the current month's Ledger until dismissed (null: nothing to show). */
    recap: MonthRecap?,
    /** The trip on today, shown on the current month's Ledger. */
    ongoingTrip: TripSummary?,
    onOpenTrip: (String) -> Unit,
    /** Opens the month's full list. */
    onOpenMonthList: (YearMonth) -> Unit,
    onOpenRecap: (YearMonth) -> Unit,
    onDismissRecap: (YearMonth) -> Unit,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onCategory: (Category, FoodType?) -> Unit,
    onSearch: () -> Unit,
    reminderOn: Boolean,
    owlsBlocked: Boolean,
    onFixOwls: () -> Unit,
    snackbar: SnackbarHostState,
    bottomBar: @Composable () -> Unit,
) {
    val w = LocalWords.current
    // The Ledger lists the latest few expenses; "Show all" opens the whole month until the month changes.
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = bottomBar,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground, // a transparent scaffold would otherwise default to black text
        topBar = {
            WizardTopBar(
                title = "Expensiarmus",
                icon = R.drawable.emblem_logo_pouch,
                actions = {
                    // Only away from the current month: the Time-Turner brings you back to it.
                    val now = YearMonth.now()
                    if (summary.month != now) {
                        IconButton(onClick = onThisMonth) { TodayGlyph(w.thisMonth(now.format(monthFormat))) }
                    }
                    IconButton(onClick = onSearch) {
                        SearchGlyph()
                    }
                },
            )
        },
        floatingActionButton = {
            WizardFab(text = w.fab.substringAfter(' '), onClick = onAdd) {
                Icon(Icons.Default.Create, contentDescription = null)
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (reminderOn && owlsBlocked) {
                item { OwlsBlockedBanner(onFixOwls) }
            }
            item { MonthSwitcher(summary, onPreviousMonth, onNextMonth) }
            when (view) {
                MonthView.OVERVIEW -> {
                    recap?.let { r -> item { RecapCard(r, onOpenRecap, onDismissRecap) } }
                    ongoingTrip?.let { t -> item { TripCard(t, onClick = { onOpenTrip(t.trip.id) }, showDates = false) } }
                    item { TotalCard(summary) }
                    item { BreakdownCard(summary.categories, summary.totalCents, summary.month, onCategory, period = summary.month.toString()) }
                    if (summary.food.totalCents > 0) {
                        item { FoodCard(summary.food) { onCategory(Category.ENTERTAINMENT, it) } }
                    }
                    item { OrnamentHeading(w.ledger) }
                    if (summary.expenses.isEmpty()) {
                        item { Hint(w.ledgerEmpty) }
                    }
                    items(ExpenseQueries.ledgerPreview(summary.expenses, false), key = { it.id }) { expense ->
                        ExpenseRow(expense, onClick = { onOpen(expense.id) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    // The latest few here; the month's full list (with or without trips) is its own page.
                    if (summary.expenses.isNotEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                TextButton(onClick = { onOpenMonthList(summary.month) }) {
                                    Text(if (summary.expenses.size > ExpenseQueries.LATEST_COUNT) "Show all ${summary.expenses.size} ›" else "Open the full list ›")
                                }
                            }
                        }
                    }
                }
                MonthView.CALENDAR -> {
                    item { GlanceCard(days.glance, summary.month) }
                    item {
                        ParchmentCard {
                            CalendarGrid(summary.month, days.totals, days.selected, onSelectDay)
                        }
                    }
                    val day = days.selected
                    if (day == null) {
                        item { Hint(w.dayHint) }
                    } else {
                        item {
                            OrnamentHeading(day.format(longDayFormat))
                            Text(
                                "Spent: ${Money.format(days.expenses.sumOf { it.myShareCents })}",
                                style = MaterialTheme.typography.titleSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        }
                        if (days.expenses.isEmpty()) {
                            item { Hint(w.dayEmpty) }
                        }
                        items(days.expenses, key = { "day-" + it.id }) { expense ->
                            ExpenseRow(expense, onClick = { onOpen(expense.id) })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

/** Shown when the 21:30 owl can't appear because notifications are off for the app. */
@Composable
private fun OwlsBlockedBanner(onFix: () -> Unit) {
    Card(
        onClick = onFix,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        Text(
            "🦉 Owl reminders are blocked: tap to allow notifications",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(8.dp),
    )
}

@Composable
private fun MonthSwitcher(summary: MonthlySummary, onPrevious: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous month",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            summary.month.format(monthFormat),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next month",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
    }
}

private val shortDayFormat = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH)

/** Above the calendar: the month's total, how it compares with the month before, and three small facts. */
@Composable
private fun GlanceCard(g: MonthGlance, month: YearMonth) {
    val w = LocalWords.current
    ParchmentCard(spacing = Arrangement.spacedBy(4.dp)) {
        Text("${w.spentInPrefix} ${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)}", style = MaterialTheme.typography.titleSmall)
        Text(Money.format(g.totalCents), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        val before = g.upToDay?.let { "${g.previousMonth.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} 1–$it" }
            ?: g.previousMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        g.changePercent?.takeIf { g.totalCents > 0 }?.let { change ->
            Text(
                when {
                    change > 0 -> "▲ $change% more than $before"
                    change < 0 -> "▼ ${-change}% less than $before"
                    else -> "Same as $before"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    change > 0 -> MaterialTheme.colorScheme.error
                    change < 0 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlanceTile("${g.count}", if (g.count == 1) "expense" else "expenses")
            GlanceTile(Money.format(g.dailyAverageCents), if (g.upToDay != null) "a day so far" else "a day")
            GlanceTile(
                g.busiestDay?.format(shortDayFormat) ?: "–",
                if (g.busiestDay != null) "busiest · ${Money.format(g.busiestCents)}" else "busiest day",
            )
        }
    }
}

@Composable
private fun RowScope.GlanceTile(value: String, label: String) {
    Column(
        Modifier
            .weight(1f)
            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** The end-of-month owl on the Ledger: last month in one line, with a way to open it and to dismiss the card. */
@Composable
private fun RecapCard(r: MonthRecap, onOpen: (YearMonth) -> Unit, onDismiss: (YearMonth) -> Unit) {
    val w = LocalWords.current
    ParchmentCard(spacing = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmblemBadge(R.drawable.emblem_set_reminder, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Text(w.recapHeading, style = MaterialTheme.typography.titleMedium)
        }
        Text(r.text { if (w.nicknames) it.nickname else it.label }, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onOpen(r.month) }) { Text("Open ${MonthRecap.monthName(r.month)} ›") }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { onDismiss(r.month) }) { Text("Dismiss") }
        }
    }
}

@Composable
private fun TotalCard(summary: MonthlySummary) {
    ParchmentCard(spacing = Arrangement.spacedBy(4.dp)) {
        Text(LocalWords.current.spentThisMonth, style = MaterialTheme.typography.titleSmall)
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
        // The whole-month ward, when one is cast.
        summary.totalGoal?.let { goal ->
            GoalBar(goal, Modifier.padding(top = 4.dp))
            Text(goal.describe(LocalWords.current), style = MaterialTheme.typography.bodySmall)
        }
    }
}


@Composable
internal fun BreakdownCard(
    categories: List<CategoryTotal>,
    totalCents: Long,
    resetKey: Any,
    onCategory: (Category, FoodType?) -> Unit,
    /** What a tapped category opens for: the month, the year, or "" for all time. */
    period: String = "",
) {
    var highlighted by remember(resetKey) { mutableStateOf<Category?>(null) }
    val glow = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    ParchmentCard(spacing = Arrangement.spacedBy(10.dp)) {
        Text(LocalWords.current.whereWent, style = MaterialTheme.typography.titleMedium)
        if (totalCents > 0) {
            DonutChart(
                slices = categories.map { it.category.color to it.spentCents.toFloat() },
                modifier = Modifier.size(200.dp).align(Alignment.CenterHorizontally),
                selected = highlighted?.let { h -> categories.indexOfFirst { it.category == h } },
                // Tapping the highlighted slice again, or the hole, shows the total again.
                onSliceTap = { i -> highlighted = i?.let { categories[it].category }?.takeIf { it != highlighted } },
            ) {
                val pick = categories.firstOrNull { it.category == highlighted }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (pick == null) {
                        Text("✦ Total ✦", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(
                            Money.format(totalCents),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    } else {
                        EmblemBadge(pick.category.icon, size = 30.dp)
                        Text(pick.category.label, style = MaterialTheme.typography.titleSmall)
                        Text(Money.format(pick.spentCents), fontWeight = FontWeight.Bold)
                        Text(
                            "${Math.round(pick.shareOfMonth * 100)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        categories.forEach { c ->
            val empty = c.spentCents == 0L && c.goal == null
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (c.category == highlighted) glow else Color.Transparent)
                    .clickable { onCategory(c.category, null) }
                    .alpha(if (empty || (highlighted != null && c.category != highlighted)) 0.55f else 1f)
                    .padding(vertical = 4.dp, horizontal = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmblemBadge(c.category.icon, size = 30.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.category.label, style = MaterialTheme.typography.titleSmall)
                        if (LocalWords.current.nicknames) {
                            Text(
                                c.category.nickname,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(Money.format(c.spentCents), fontWeight = FontWeight.SemiBold)
                        Text(
                            "${Math.round(c.shareOfMonth * 100)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Show ${c.category.label} expenses",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                c.goal?.let { goal ->
                    Spacer(Modifier.height(4.dp))
                    GoalBar(goal, Modifier.padding(start = 40.dp, end = 24.dp), height = 8.dp)
                    Text(
                        goal.describe(LocalWords.current),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 40.dp, top = 2.dp),
                    )
                }
            }
        }
        Text(
            "Tap a slice to highlight it, or a ${LocalWords.current.vault} to see its expenses ${ExpenseQueries.periodWords(period)}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun FoodCard(food: FoodSummary, onFoodType: (FoodType) -> Unit) {
    ParchmentCard(spacing = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(LocalWords.current.food, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(Money.format(food.totalCents), fontWeight = FontWeight.Bold)
        }
        // One bar split into the food types, widths proportional to spending.
        Row(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp)),
        ) {
            food.types.filter { it.spentCents > 0 }.forEach { t ->
                Box(
                    Modifier
                        .weight(t.spentCents.toFloat())
                        .fillMaxHeight()
                        .background(t.type.color),
                )
            }
        }
        food.types.forEach { t ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onFoodType(t.type) }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmblemBadge(t.type.icon, size = 30.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.type.label, style = MaterialTheme.typography.titleSmall)
                    Text(
                        (if (LocalWords.current.nicknames) t.type.nickname + " · " else "") + if (t.count == 1) "1 time" else "${t.count} times",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(Money.format(t.spentCents), fontWeight = FontWeight.SemiBold)
                    Text(
                        "${Math.round(t.shareOfFood * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Show ${t.type.label} expenses",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (food.otherEntertainmentCents > 0) {
            Text(
                "Other entertainment (not food): ${Money.format(food.otherEntertainmentCents)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
