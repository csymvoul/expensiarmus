package com.csymvoul.expenses.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.ui.components.ColorDot
import com.csymvoul.expenses.ui.theme.color
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMMM yyyy", Locale.ENGLISH)

/** Adds a new expense when [existing] is null, otherwise edits it. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditExpenseScreen(
    existing: Expense?,
    defaultDate: LocalDate,
    onSave: (Expense) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var amount by rememberSaveable { mutableStateOf(existing?.let { Money.forInput(it.amountCents) } ?: "") }
    var date by rememberSaveable { mutableStateOf(existing?.date ?: defaultDate) }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: Category.SUPERMARKET) }
    var foodType by rememberSaveable { mutableStateOf(existing?.foodType) }
    var split by rememberSaveable { mutableStateOf(existing?.isSplit ?: false) }
    var myPercent by rememberSaveable { mutableIntStateOf(existing?.takeIf { it.isSplit }?.myPercent ?: 50) }
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    var confirmingDelete by remember { mutableStateOf(false) }

    val cents = Money.parseToCents(amount)
    val titleOk = title.isNotBlank()
    val amountOk = cents != null && cents > 0

    fun save() {
        if (titleOk && amountOk) {
            onSave(
                Expense(
                    id = existing?.id ?: UUID.randomUUID().toString(),
                    date = date,
                    title = title.trim(),
                    amountCents = cents!!,
                    category = category,
                    myPercent = if (split) myPercent.coerceIn(1, 99) else 100,
                    foodType = if (category == Category.ENTERTAINMENT) foodType else null,
                ),
            )
        } else {
            showErrors = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "Add expense" else "Edit expense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (existing != null) {
                        IconButton(onClick = { confirmingDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                    IconButton(onClick = ::save) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                isError = showErrors && !titleOk,
                supportingText = if (showErrors && !titleOk) ({ Text("Enter a title") }) else null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount spent") },
                prefix = { Text("€ ") },
                singleLine = true,
                isError = showErrors && !amountOk,
                supportingText = if (showErrors && !amountOk) ({ Text("Enter an amount, e.g. 12.50") }) else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(date.format(dateFormat))
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Category", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.entries.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c.label) },
                            leadingIcon = { ColorDot(c.color, size = 10.dp) },
                        )
                    }
                }
                Text(
                    category.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (category == Category.ENTERTAINMENT) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Is it food?", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = foodType == null,
                            onClick = { foodType = null },
                            label = { Text("Not food") },
                        )
                        FoodType.entries.forEach { t ->
                            FilterChip(
                                selected = foodType == t,
                                onClick = { foodType = t },
                                label = { Text(t.label) },
                                leadingIcon = { ColorDot(t.color, size = 10.dp) },
                            )
                        }
                    }
                    Text(
                        foodType?.hint ?: "Cinema, theatre, concerts, etc.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Split with someone else", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Only your share counts toward totals and goals",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = split, onCheckedChange = { split = it })
            }
            if (split) {
                Column {
                    Text("My share: $myPercent%  ·  Theirs: ${100 - myPercent}%")
                    Slider(
                        value = myPercent.toFloat(),
                        onValueChange = { myPercent = it.roundToInt().coerceIn(1, 99) },
                        valueRange = 1f..99f,
                        steps = 97,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(50, 33, 25).forEach { p ->
                            AssistChip(
                                onClick = { myPercent = p },
                                label = { Text(if (p == 50) "50-50" else "$p%") },
                            )
                        }
                    }
                    if (amountOk) {
                        val share = (cents!! * myPercent + 50) / 100
                        Text(
                            "You pay ${Money.format(share)} of ${Money.format(cents)}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            Button(onClick = ::save, modifier = Modifier.fillMaxWidth()) {
                Text("Save")
            }
        }
    }

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }

    if (confirmingDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Delete expense?") },
            text = { Text("\"${existing.title}\" will be removed. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmingDelete = false; onDelete(existing.id) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Cancel") } },
        )
    }
}
