package com.csymvoul.expenses.ui.screens

import com.csymvoul.expenses.ui.components.ChipEmblem
import com.csymvoul.expenses.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.domain.TripStatus
import com.csymvoul.expenses.domain.TripSummary
import com.csymvoul.expenses.model.Trip
import com.csymvoul.expenses.ui.components.EmblemBadge
import com.csymvoul.expenses.ui.components.ExpenseRow
import com.csymvoul.expenses.ui.components.GoalBar
import com.csymvoul.expenses.ui.components.LocalWords
import com.csymvoul.expenses.ui.components.OrnamentHeading
import com.csymvoul.expenses.ui.components.ParchmentCard
import com.csymvoul.expenses.ui.components.WizardButton
import com.csymvoul.expenses.ui.components.WizardTopBar
import com.csymvoul.expenses.ui.theme.icon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val longDate = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** "20 Nov – 28 Nov 2026", as on the web. */
fun tripDates(t: Trip): String = if (t.start == t.end) t.start.format(longDate) else "${t.start.format(shortDate)} – ${t.end.format(longDate)}"

/** "in 3 days", "day 3 of 9", "9 days". */
fun tripStatusText(s: TripStatus): String = when (s.state) {
    TripStatus.State.UPCOMING -> if (s.inDays == 1) "tomorrow" else "in ${s.inDays} days"
    TripStatus.State.ONGOING -> "day ${s.day} of ${s.days}"
    TripStatus.State.DONE -> if (s.days == 1) "1 day" else "${s.days} days"
}

/** Trip chips for recording: tap one to tie the expense to it, tap it again to untie. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun TripChips(trips: List<Trip>, current: String?, onPick: (String?) -> Unit) {
    if (trips.isEmpty()) return
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            ChipEmblem(R.drawable.emblem_trip)
            Spacer(Modifier.width(4.dp))
            Text(LocalWords.current.trip, style = MaterialTheme.typography.titleSmall)
        }
        trips.forEach { t ->
            androidx.compose.material3.FilterChip(
                selected = current == t.id,
                onClick = { onPick(if (current == t.id) null else t.id) },
                label = { Text(t.name) },
            )
        }
    }
}

/** A trip as a card: name, dates and where it stands, total, and the budget bar. Used in Annals and on the Ledger. */
@Composable
fun TripCard(s: TripSummary, onClick: () -> Unit, showDates: Boolean = true) {
    val w = LocalWords.current
    ParchmentCard(Modifier.clickable(onClick = onClick), spacing = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmblemBadge(R.drawable.emblem_trip, size = 34.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(s.trip.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    (if (showDates) tripDates(s.trip) + " · " else "") + tripStatusText(s.status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(s.totalCents), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                s.budget?.let { Text("of ${Money.format(it.goalCents)}", style = MaterialTheme.typography.bodySmall) }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
        s.budget?.let {
            GoalBar(it)
            Text(it.describe(w), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** One trip: total against its budget, per day, per category, and its expenses. */
@Composable
fun TripScreen(s: TripSummary?, onOpen: (String) -> Unit, onEditTrips: () -> Unit, onBack: () -> Unit) {
    val w = LocalWords.current
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            WizardTopBar(
                title = s?.trip?.name ?: w.trips,
                icon = R.drawable.emblem_trip,
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        if (s == null) return@Scaffold
        LazyColumn(
            contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ParchmentCard(spacing = Arrangement.spacedBy(6.dp)) {
                    Text("${tripDates(s.trip)} · ${tripStatusText(s.status)}", style = MaterialTheme.typography.labelLarge)
                    Text(Money.format(s.totalCents), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    s.budget?.let {
                        GoalBar(it)
                        Text(it.describe(w), style = MaterialTheme.typography.bodySmall)
                    } ?: Text("No budget set.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        (if (s.expenses.size == 1) "1 expense" else "${s.expenses.size} expenses") + " · ${Money.format(s.perDayCents)} a day" +
                            if (s.status.state == TripStatus.State.ONGOING) " so far" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (s.byCategory.isNotEmpty()) {
                item {
                    ParchmentCard(spacing = Arrangement.spacedBy(8.dp)) {
                        Text(w.whereWent, style = MaterialTheme.typography.titleMedium)
                        s.byCategory.forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                EmblemBadge(c.foodType?.icon ?: c.category.icon, size = 30.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(c.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(Money.format(c.cents), fontWeight = FontWeight.SemiBold)
                                    Text("${if (s.totalCents > 0) Math.round(c.cents * 100.0 / s.totalCents) else 0}%", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                item { OrnamentHeading(w.ledger) }
                items(s.expenses, key = { it.id }) { e ->
                    ExpenseRow(e, onClick = { onOpen(e.id) }, showYear = true)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            } else {
                item {
                    Text(
                        "Nothing recorded for this ${w.trip.lowercase()} yet. Pick it when you record an expense.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
            item {
                TextButton(onClick = onEditTrips, modifier = Modifier.fillMaxWidth()) { Text("Edit ${w.trip.lowercase()} ›") }
            }
        }
    }
}

/** Settings → Trips: add, change or delete trips. [onSave] returns what to fix, or null when saved. */
@Composable
fun TripsScreen(trips: List<Trip>, onSave: (Trip) -> String?, onDelete: (String) -> Unit, onBack: () -> Unit) {
    val w = LocalWords.current
    // null: nothing open; "": a new trip; otherwise the id being edited.
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            WizardTopBar(
                title = w.trips,
                icon = R.drawable.emblem_trip,
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Give each trip its dates and, if you like, one total budget. While a trip is on it is ticked when you record; " +
                    "its expenses keep their category and still count in their month. See them in Annals → ${w.trips}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (trips.isEmpty() && open == null) {
                Text("No ${w.trips.lowercase()} yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
            }
            trips.forEach { t ->
                ParchmentCard {
                    if (open == t.id) {
                        TripEditor(t, onSave = { onSave(it).also { err -> if (err == null) open = null } }, onDelete = { onDelete(t.id); open = null }, onCancel = { open = null })
                    } else {
                        Row(Modifier.fillMaxWidth().clickable { open = t.id }, verticalAlignment = Alignment.CenterVertically) {
                            EmblemBadge(R.drawable.emblem_trip, size = 34.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    tripDates(t) + (t.budgetCents?.let { " · budget ${Money.format(it)}" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
            }
            if (open == "") {
                ParchmentCard {
                    Text(w.newTrip, style = MaterialTheme.typography.titleMedium)
                    val today = remember { LocalDate.now() }
                    TripEditor(
                        Trip(UUID.randomUUID().toString(), "", today, today),
                        onSave = { onSave(it).also { err -> if (err == null) open = null } },
                        onDelete = null,
                        onCancel = { open = null },
                    )
                }
            } else if (open == null) {
                WizardButton(onClick = { open = "" }, modifier = Modifier.fillMaxWidth()) {
                    EmblemBadge(R.drawable.emblem_trip, size = 24.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(w.newTrip)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripEditor(start: Trip, onSave: (Trip) -> String?, onDelete: (() -> Unit)?, onCancel: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(start.name) }
    var first by rememberSaveable { mutableStateOf(start.start) }
    var last by rememberSaveable { mutableStateOf(start.end) }
    var budget by rememberSaveable { mutableStateOf(start.budgetCents?.let(Money::forInput) ?: "") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var picking by remember { mutableStateOf<String?>(null) } // "first" or "last"
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name, onValueChange = { name = it }, label = { Text("Name") }, placeholder = { Text("e.g. Crete 2027") }, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picking = "first" }, modifier = Modifier.weight(1f)) { Text("From ${first.format(longDate)}") }
            OutlinedButton(onClick = { picking = "last" }, modifier = Modifier.weight(1f)) { Text("To ${last.format(longDate)}") }
        }
        OutlinedTextField(
            value = budget, onValueChange = { budget = it }, label = { Text("Budget (optional)") }, prefix = { Text("€ ") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        if (confirmDelete) Text("Delete $name? Its expenses stay, no longer tied to it.")
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onDelete != null) {
                TextButton(onClick = { if (confirmDelete) onDelete() else confirmDelete = true }) {
                    Text(if (confirmDelete) "Delete it" else "Delete", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("Cancel") }
            if (!confirmDelete) {
                WizardButton(
                    onClick = {
                        val cents = budget.trim().takeIf { it.isNotEmpty() }?.let(Money::parseToCents)
                        error = if (budget.isNotBlank() && cents == null) "The budget must be an amount, or left empty."
                        else onSave(start.copy(name = name, start = first, end = last, budgetCents = cents))
                    },
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                ) { Text("Save") }
            }
        }
    }
    picking?.let { which ->
        val state = rememberDatePickerState(initialSelectedDateMillis = (if (which == "first") first else last).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        if (which == "first") { first = d; if (last < d) last = d } else last = d
                    }
                    picking = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}
