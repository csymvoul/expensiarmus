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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.domain.FoodSummary
import com.csymvoul.expenses.domain.GoalProgress
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.domain.MonthlySummary
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.ui.components.ColorDot
import com.csymvoul.expenses.ui.components.DonutChart
import com.csymvoul.expenses.ui.components.GoalBar
import com.csymvoul.expenses.ui.theme.color
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(
    summary: MonthlySummary,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onGoals: () -> Unit,
    onExport: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses") },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Monthly goals") },
                                onClick = { menuOpen = false; onGoals() },
                            )
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
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
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add expense") },
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
            item { BreakdownCard(summary) }
            if (summary.food.totalCents > 0) {
                item { FoodCard(summary.food) }
            }
            item {
                Text(
                    "Expenses (${summary.expenses.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (summary.expenses.isEmpty()) {
                item {
                    Text(
                        "Nothing recorded for this month yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(summary.expenses, key = { it.id }) { expense ->
                ExpenseRow(expense, onClick = { onOpen(expense.id) })
                HorizontalDivider()
            }
        }
    }
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
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun TotalCard(summary: MonthlySummary, onGoals: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Spent this month", style = MaterialTheme.typography.labelLarge)
            Text(
                Money.format(summary.totalCents),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            if (summary.paidTotalCents != summary.totalCents) {
                Text(
                    "Your share. Full bills came to ${Money.format(summary.paidTotalCents)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            val goal = summary.overall
            if (goal != null) {
                GoalBar(goal)
                Spacer(Modifier.height(6.dp))
                Text(goalText(goal), style = MaterialTheme.typography.bodyMedium)
            } else {
                TextButton(onClick = onGoals, contentPadding = PaddingValues(0.dp)) {
                    Text("Set a monthly goal")
                }
            }
        }
    }
}

internal fun goalText(goal: GoalProgress): String {
    val tail = if (goal.isOver) {
        "${Money.format(-goal.remainingCents)} over"
    } else {
        "${Money.format(goal.remainingCents)} left"
    }
    return "${goal.percent}% of ${Money.format(goal.goalCents)} · $tail"
}

@Composable
private fun BreakdownCard(summary: MonthlySummary) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("By category", style = MaterialTheme.typography.titleMedium)
            if (summary.totalCents > 0) {
                DonutChart(
                    slices = summary.categories.map { it.category.color to it.spentCents.toFloat() },
                    modifier = Modifier.size(180.dp).align(Alignment.CenterHorizontally),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", style = MaterialTheme.typography.labelMedium)
                        Text(
                            Money.format(summary.totalCents),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            val shown = summary.categories.filter { it.spentCents > 0 || it.goal != null }
            if (shown.isEmpty()) {
                Text("No spending yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            shown.forEach { c ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(c.category.color)
                        Spacer(Modifier.width(8.dp))
                        Text(c.category.label, modifier = Modifier.weight(1f))
                        Text(Money.format(c.spentCents), fontWeight = FontWeight.SemiBold)
                        Text(
                            "${Math.round(c.shareOfMonth * 100)}%",
                            modifier = Modifier.width(48.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    c.goal?.let { goal ->
                        Spacer(Modifier.height(4.dp))
                        GoalBar(goal, Modifier.padding(start = 20.dp), height = 6.dp)
                        Text(
                            goalText(goal),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 20.dp, top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodCard(food: FoodSummary) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Food", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(t.type.color)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.type.label)
                        Text(
                            if (t.count == 1) "1 time" else "${t.count} times",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(Money.format(t.spentCents), fontWeight = FontWeight.SemiBold)
                    Text(
                        "${Math.round(t.shareOfFood * 100)}%",
                        modifier = Modifier.width(48.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
}

@Composable
private fun ExpenseRow(expense: Expense, onClick: () -> Unit) {
    val details = buildString {
        append(expense.date.format(dayFormat))
        append(" · ").append(expense.category.label)
        expense.foodType?.let { append(" › ").append(it.label) }
        if (expense.isSplit) append(" · my share ${expense.myPercent}%")
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { ColorDot(expense.category.color) },
        headlineContent = { Text(expense.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(details) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Money.format(expense.myShareCents),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (expense.isSplit) {
                    Text("of ${Money.format(expense.amountCents)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
    )
}
