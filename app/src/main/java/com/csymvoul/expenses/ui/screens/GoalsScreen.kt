package com.csymvoul.expenses.ui.screens

import com.csymvoul.expenses.ui.components.ChipEmblem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.domain.GoalProgress
import com.csymvoul.expenses.domain.GoalScope
import com.csymvoul.expenses.R
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.GoalEntry
import com.csymvoul.expenses.model.Goals
import com.csymvoul.expenses.ui.components.EmblemBadge
import com.csymvoul.expenses.ui.components.TodayGlyph
import com.csymvoul.expenses.ui.components.GoalBar
import com.csymvoul.expenses.ui.components.LocalWords
import com.csymvoul.expenses.ui.components.ParchmentCard
import com.csymvoul.expenses.ui.components.WizardButton
import com.csymvoul.expenses.ui.components.WizardTopBar
import com.csymvoul.expenses.ui.theme.color
import com.csymvoul.expenses.ui.theme.icon
import com.csymvoul.expenses.ui.components.Tab
import com.csymvoul.expenses.domain.Words
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthName = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
private val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

/**
 * Protego: the goals ("wards") of one month: one for the whole month and one per category. Tap a ward to change or remove it, or cast a
 * new one for a category, for this month only or for this month and every month after.
 */
@Composable
fun GoalsScreen(
    month: YearMonth,
    goals: Goals,
    /** Spent per category, and under null the whole month's total. */
    spent: Map<Category?, Long>,
    onSet: (Category?, Long, Boolean) -> Unit,
    onRemove: (Category?, Boolean) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onThisMonth: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val w = LocalWords.current
    // Wards are keyed "TOTAL" (the whole month) or by category code, see GoalScope.
    var editing by rememberSaveable(month) { mutableStateOf<String?>(null) }
    var adding by rememberSaveable(month) { mutableStateOf(false) }
    val active = GoalScope.targets.mapNotNull { c -> goals.goalFor(c, month)?.let { c to it } }
    val free = GoalScope.targets.filter { c -> active.none { it.first == c } }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            WizardTopBar(
                title = "${w.goals} · ${month.format(monthName)}",
                icon = Tab.GOALS.icon,
                actions = {
                    // Only away from the current month: the Time-Turner brings you back to it.
                    val now = YearMonth.now()
                    if (month != now) {
                        IconButton(onClick = onThisMonth) { TodayGlyph(w.thisMonth(now.format(monthYear))) }
                    }
                },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = MaterialTheme.colorScheme.primary)
                }
                Text(
                    month.format(monthYear),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                w.goalsIntro(month.format(monthYear)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (active.isEmpty() && !adding) {
                Text(
                    w.noGoals,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
            }
            active.forEach { (category, entry) ->
                val wardKey = GoalScope.key(category)
                key(wardKey) {
                    ParchmentCard(Modifier.clickable(enabled = editing != wardKey) { editing = wardKey; adding = false }) {
                        WardSummary(category, entry, spent[category] ?: 0L, month)
                        if (editing == wardKey) {
                            WardEditor(
                                month = month,
                                fixed = true,
                                fixedCategory = category,
                                choices = emptyList(),
                                initialCents = entry.cents,
                                initialOnwards = entry.onwards,
                                onSave = { c, cents, onwards -> onSet(c, cents, onwards); editing = null },
                                onCancel = { editing = null },
                                onRemove = { onwards -> onRemove(category, onwards); editing = null },
                            )
                        }
                    }
                }
            }
            if (adding) {
                ParchmentCard {
                    Text(w.aNewWard, style = MaterialTheme.typography.titleMedium)
                    WardEditor(
                        month = month,
                        fixed = false,
                        fixedCategory = null,
                        choices = free,
                        initialCents = null,
                        initialOnwards = false,
                        onSave = { c, cents, onwards -> onSet(c, cents, onwards); adding = false },
                        onCancel = { adding = false },
                        onRemove = null,
                    )
                }
            } else if (free.isNotEmpty()) {
                WizardButton(onClick = { adding = true; editing = null }, modifier = Modifier.fillMaxWidth()) {
                    EmblemBadge(Tab.GOALS.icon, size = 26.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(Words.withoutEmoji(w.newWard))
                }
            }
        }
    }
}

@Composable
private fun WardSummary(category: Category?, entry: GoalEntry, spentCents: Long, month: YearMonth) {
    val w = LocalWords.current
    val progress = GoalProgress(entry.cents, spentCents)
    Row(verticalAlignment = Alignment.CenterVertically) {
        EmblemBadge(category?.icon ?: R.drawable.emblem_logo_pouch, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(category?.label ?: w.wholeMonth, style = MaterialTheme.typography.titleSmall)
            Text(
                (if (category == null) "${w.wholeMonthSub} · " else "") + GoalScope.text(entry, month),
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(Money.format(entry.cents), style = MaterialTheme.typography.titleMedium)
    }
    GoalBar(progress)
    Text(progress.describe(LocalWords.current), style = MaterialTheme.typography.bodySmall)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WardEditor(
    month: YearMonth,
    /** True when changing an existing ward (no picker); false when casting a new one. */
    fixed: Boolean,
    fixedCategory: Category?,
    choices: List<Category?>,
    initialCents: Long?,
    initialOnwards: Boolean,
    onSave: (Category?, Long, Boolean) -> Unit,
    onCancel: () -> Unit,
    onRemove: ((Boolean) -> Unit)?,
) {
    var pickedKey by rememberSaveable {
        mutableStateOf(if (fixed) GoalScope.key(fixedCategory) else if (choices.isEmpty()) null else GoalScope.key(choices.first()))
    }
    var amount by rememberSaveable { mutableStateOf(initialCents?.let(Money::forInput) ?: "") }
    var onwards by rememberSaveable { mutableStateOf(initialOnwards) }
    var confirmRemove by remember { mutableStateOf(false) }
    val cents = Money.parseToCents(amount)
    val amountOk = cents != null && cents > 0
    val name = month.format(monthName)

    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!fixed) {
            Text(LocalWords.current.whichCategory, style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                choices.forEach { c ->
                    FilterChip(
                        selected = pickedKey == GoalScope.key(c),
                        onClick = { pickedKey = GoalScope.key(c) },
                        label = { Text(c?.label ?: LocalWords.current.wholeMonth) },
                        leadingIcon = { ChipEmblem(c?.icon ?: R.drawable.emblem_logo_pouch) },
                    )
                }
            }
        }
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Goal for $name") },
            prefix = { Text("€ ") },
            singleLine = true,
            isError = amount.isNotBlank() && !amountOk,
            supportingText = if (amount.isNotBlank() && !amountOk) ({ Text("Enter an amount, e.g. 60") }) else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Every month from $name on", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (onwards) "Also for the months after $name" else "Only ${month.format(monthYear)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = onwards, onCheckedChange = { onwards = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onRemove != null) {
                TextButton(onClick = { confirmRemove = true }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("Cancel") }
            WizardButton(
                onClick = { pickedKey?.let { k -> cents?.let { onSave(GoalScope.target(k), it, onwards) } } },
                enabled = pickedKey != null && amountOk,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            ) { Text("Save") }
        }
    }

    if (confirmRemove && onRemove != null) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(LocalWords.current.removeWard) },
            text = { Text("Remove it for $name only, or for $name and every month after?") },
            confirmButton = {
                Row {
                    TextButton(onClick = { confirmRemove = false; onRemove(false) }) { Text("Only $name") }
                    TextButton(onClick = { confirmRemove = false; onRemove(true) }) { Text("From $name on") }
                }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Keep it") } },
        )
    }
}
