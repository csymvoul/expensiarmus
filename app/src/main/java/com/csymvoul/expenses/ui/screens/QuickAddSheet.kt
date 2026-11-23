package com.csymvoul.expenses.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.domain.MoneyStyle
import com.csymvoul.expenses.domain.Suggestion
import com.csymvoul.expenses.domain.Suggestions
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.ui.components.EmblemBadge
import com.csymvoul.expenses.ui.components.LocalWords
import com.csymvoul.expenses.ui.theme.color
import com.csymvoul.expenses.ui.theme.icon
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)

/** What the full form receives from Quick add's "More details". */
data class QuickPrefill(val title: String, val amount: String, val date: LocalDate)

/**
 * Quick add: type the amount on a big number pad, then tap a category (or food) tile to save.
 * The title is optional; typing one offers earlier expenses, and tapping one records it at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    history: List<Expense>,
    date: LocalDate,
    onSave: (Expense) -> Unit,
    onMore: (QuickPrefill) -> Unit,
    onDismiss: () -> Unit,
) {
    val w = LocalWords.current
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf(date) }
    var error by remember { mutableStateOf(false) }
    var pickingDate by remember { mutableStateOf(false) }
    val suggestions = remember(history, title) { Suggestions.of(history, title) }
    val greek = Money.style == MoneyStyle.GREEK

    fun key(k: String) {
        error = false
        amount = when {
            k == "⌫" -> amount.dropLast(1)
            k == "." || k == "," -> if (amount.any { it == '.' || it == ',' }) amount else amount.ifEmpty { "0" } + k
            Regex("[.,](\\d*)$").find(amount)?.groupValues?.get(1)?.length ?: 0 >= 2 -> amount
            amount.count { it.isDigit() } >= 8 -> amount
            amount == "0" -> k
            else -> amount + k
        }
    }

    fun save(category: Category, food: FoodType?) {
        val cents = Money.parseToCents(amount)
        if (cents == null || cents <= 0) {
            error = true
            return
        }
        onSave(
            Expense(
                id = UUID.randomUUID().toString(),
                date = day,
                title = title.trim().ifEmpty { Suggestions.defaultTitle(category, food) },
                amountCents = cents,
                category = category,
                foodType = food,
            ),
        )
    }

    fun record(s: Suggestion) = onSave(
        Expense(UUID.randomUUID().toString(), day, s.title, s.amountCents, s.category, s.myPercent, s.foodType),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Text(w.newExpense, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                TextButton(onClick = { onMore(QuickPrefill(title, amount, day)) }) { Text("More details ›") }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            if (suggestions.isNotEmpty()) SuggestionList(suggestions, ::record)
            Text(
                if (greek) "${amount.ifEmpty { "0" }} €" else "€${amount.ifEmpty { "0" }}",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { pickingDate = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("📅 ${day.format(dayFormat)}")
            }
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf(Money.decimalKey(), "0", "⌫")).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { k ->
                        OutlinedButton(
                            onClick = { key(k) },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) { Text(k, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
            Text(
                if (error) "Type an amount first" else "Tap a category to save",
                style = MaterialTheme.typography.bodySmall,
                color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            val tiles = Category.entries.map { Triple(it, null as FoodType?, it.icon to it.label) } +
                FoodType.entries.map { Triple(Category.ENTERTAINMENT, it as FoodType?, it.icon to it.label) }
            tiles.chunked(5).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { (category, food, look) ->
                        Column(
                            Modifier.weight(1f).clickable { save(category, food) }.padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            EmblemBadge(look.first, size = 40.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(look.second, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
                        }
                    }
                }
            }
        }
    }

    if (pickingDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { day = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

/** Earlier expenses matching the typed title; tapping one calls [onPick]. */
@Composable
internal fun SuggestionList(suggestions: List<Suggestion>, onPick: (Suggestion) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        suggestions.forEach { s ->
            Row(
                Modifier.fillMaxWidth().clickable { onPick(s) }.padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmblemBadge(s.foodType?.icon ?: s.category.icon, size = 28.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        (s.foodType?.label ?: s.category.label) + if (s.myPercent < 100) " · split ${s.myPercent}%" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(Money.format(s.amountCents), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
