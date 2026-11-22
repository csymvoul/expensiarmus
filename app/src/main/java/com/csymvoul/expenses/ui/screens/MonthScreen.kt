package com.csymvoul.expenses.ui.screens

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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.csymvoul.expenses.domain.FoodSummary
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.domain.MonthlySummary
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.ui.components.CalendarGrid
import com.csymvoul.expenses.ui.components.EmblemBadge
import com.csymvoul.expenses.ui.components.WizardTopBar
import com.csymvoul.expenses.ui.components.DonutChart
import com.csymvoul.expenses.ui.components.ExpenseRow
import com.csymvoul.expenses.ui.components.GoalBar
import com.csymvoul.expenses.ui.components.OrnamentHeading
import com.csymvoul.expenses.ui.components.ParchmentCard
import com.csymvoul.expenses.ui.theme.color
import com.csymvoul.expenses.ui.theme.emoji
import com.csymvoul.expenses.ui.theme.nickname
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val longDayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ENGLISH)

enum class MonthView { OVERVIEW, CALENDAR }

/** What the calendar shows below the grid. */
data class DayDetails(
    val totals: Map<LocalDate, Long>,
    val selected: LocalDate?,
    val expenses: List<Expense>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    summary: MonthlySummary,
    view: MonthView,
    onViewChange: (MonthView) -> Unit,
    days: DayDetails,
    onSelectDay: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onCategory: (Category, FoodType?) -> Unit,
    onSearch: () -> Unit,
    onGoals: () -> Unit,
    reminderOn: Boolean,
    onToggleReminder: () -> Unit,
    onExport: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            WizardTopBar(
                title = "Expensiarmus",
                actions = {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("🛡 Protego · monthly goals") },
                                onClick = { menuOpen = false; onGoals() },
                            )
                            DropdownMenuItem(
                                text = { Text("🦉 Owl reminder at 21:30") },
                                trailingIcon = { Checkbox(checked = reminderOn, onCheckedChange = null) },
                                onClick = { menuOpen = false; onToggleReminder() },
                            )
                            DropdownMenuItem(
                                text = { Text("📜 Geminio · export CSV") },
                                onClick = { menuOpen = false; onExport() },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                shape = CutCornerShape(14.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Create, contentDescription = null) },
                text = { Text("Record expense") },
            )
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
            item { MonthSwitcher(summary, onPreviousMonth, onNextMonth) }
            item { TotalCard(summary, onGoals) }
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    MonthView.entries.forEachIndexed { i, v ->
                        SegmentedButton(
                            selected = view == v,
                            onClick = { onViewChange(v) },
                            shape = SegmentedButtonDefaults.itemShape(i, MonthView.entries.size),
                        ) { Text(if (v == MonthView.OVERVIEW) "📜 Overview" else "📅 Calendar") }
                    }
                }
            }
            when (view) {
                MonthView.OVERVIEW -> {
                    item { BreakdownCard(summary, onCategory) }
                    if (summary.food.totalCents > 0) {
                        item { FoodCard(summary.food) { onCategory(Category.ENTERTAINMENT, it) } }
                    }
                    item { OrnamentHeading("The Ledger") }
                    if (summary.expenses.isEmpty()) {
                        item { Hint("The ledger is blank. Tap Record expense to write the first line.") }
                    }
                    items(summary.expenses, key = { it.id }) { expense ->
                        ExpenseRow(expense, onClick = { onOpen(expense.id) })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                MonthView.CALENDAR -> {
                    item {
                        ParchmentCard {
                            CalendarGrid(summary.month, days.totals, days.selected, onSelectDay)
                        }
                    }
                    val day = days.selected
                    if (day == null) {
                        item { Hint("Tap a day and say Revelio.") }
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
                            item { Hint("No gold left your vault this day.") }
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
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        Text(
            summary.month.format(monthFormat),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun TotalCard(summary: MonthlySummary, onGoals: () -> Unit) {
    ParchmentCard(spacing = Arrangement.spacedBy(4.dp)) {
        Text("Gold spent this month", style = MaterialTheme.typography.labelLarge)
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
        Spacer(Modifier.height(8.dp))
        val goal = summary.overall
        if (goal != null) {
            GoalBar(goal)
            Spacer(Modifier.height(2.dp))
            Text(goal.describe(), style = MaterialTheme.typography.bodyMedium)
        } else {
            TextButton(onClick = onGoals, contentPadding = PaddingValues(0.dp)) {
                Text("🛡 Cast Protego: set a monthly goal")
            }
        }
    }
}


@Composable
private fun BreakdownCard(summary: MonthlySummary, onCategory: (Category, FoodType?) -> Unit) {
    ParchmentCard(spacing = Arrangement.spacedBy(10.dp)) {
        Text("Where the gold went", style = MaterialTheme.typography.titleMedium)
        if (summary.totalCents > 0) {
            DonutChart(
                slices = summary.categories.map { it.category.color to it.spentCents.toFloat() },
                modifier = Modifier.size(180.dp).align(Alignment.CenterHorizontally),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✦ Total ✦", style = MaterialTheme.typography.labelMedium)
                    Text(
                        Money.format(summary.totalCents),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        summary.categories.forEach { c ->
            val empty = c.spentCents == 0L && c.goal == null
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onCategory(c.category, null) }
                    .alpha(if (empty) 0.55f else 1f)
                    .padding(vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmblemBadge(c.category.emoji, c.category.color, size = 30.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(c.category.label)
                        Text(
                            c.category.nickname,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(Money.format(c.spentCents), fontWeight = FontWeight.SemiBold)
                    Text(
                        "${Math.round(c.shareOfMonth * 100)}%",
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
                        goal.describe(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 40.dp, top = 2.dp),
                    )
                }
            }
        }
        Text(
            "Tap a vault to see all its expenses.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FoodCard(food: FoodSummary, onFoodType: (FoodType) -> Unit) {
    ParchmentCard(spacing = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Feasts & Food", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
                EmblemBadge(t.type.emoji, t.type.color, size = 30.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(t.type.label)
                    Text(
                        t.type.nickname + " · " + if (t.count == 1) "1 time" else "${t.count} times",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(Money.format(t.spentCents), fontWeight = FontWeight.SemiBold)
                Text(
                    "${Math.round(t.shareOfFood * 100)}%",
                    modifier = Modifier.width(44.dp),
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
