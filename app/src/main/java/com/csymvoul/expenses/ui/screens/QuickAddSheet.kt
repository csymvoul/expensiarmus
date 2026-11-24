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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.IconButton
import com.csymvoul.expenses.ui.components.CheckGlyph
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import com.csymvoul.expenses.ui.components.WizardButton
import com.csymvoul.expenses.ui.components.ChipEmblem
import com.csymvoul.expenses.ui.components.StarGlyph
import com.csymvoul.expenses.domain.ExpenseQueries
import com.csymvoul.expenses.model.Favorite
import com.csymvoul.expenses.ui.components.Tab
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

/** What the full form receives from Quick add's "More details" (with a picked favourite's category and split). */
data class QuickPrefill(
    val title: String,
    val amount: String,
    val date: LocalDate,
    val category: Category? = null,
    val foodType: FoodType? = null,
    val myPercent: Int? = null,
)

/**
 * Quick add: type the amount, pick a category tile (or a favourite), then tap the gold ✓ at the top.
 * Tiles only select, so you can change your mind before recording. Typing a title offers earlier
 * titles; tapping one fills it in (no amount is copied). Edit › on the favourites opens their page.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddSheet(
    history: List<Expense>,
    favorites: List<Favorite>,
    date: LocalDate,
    onSave: (Expense) -> Unit,
    onOpenFavorites: () -> Unit,
    onMore: (QuickPrefill) -> Unit,
    onDismiss: () -> Unit,
) {
    val w = LocalWords.current
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var day by rememberSaveable { mutableStateOf(date) }
    var error by remember { mutableStateOf<String?>(null) }
    var pickingDate by remember { mutableStateOf(false) }
    // Earlier titles, minus the one already typed in full.
    val suggestions = remember(history, title) {
        Suggestions.of(history, title).filter { ExpenseQueries.normalize(it.title) != ExpenseQueries.normalize(title) }
    }
    // What the ✓ will record: the picked category and food type, and the share (a favourite's split).
    var picked by rememberSaveable { mutableStateOf<Category?>(null) }
    var pickedFood by rememberSaveable { mutableStateOf<FoodType?>(null) }
    var share by rememberSaveable { mutableIntStateOf(100) }
    var favIndex by rememberSaveable { mutableStateOf<Int?>(null) }
    val greek = Money.style == MoneyStyle.GREEK

    fun key(k: String) {
        error = null
        amount = when {
            k == "⌫" -> amount.dropLast(1)
            k == "." || k == "," -> if (amount.any { it == '.' || it == ',' }) amount else amount.ifEmpty { "0" } + k
            Regex("[.,](\\d*)$").find(amount)?.groupValues?.get(1)?.length ?: 0 >= 2 -> amount
            amount.count { it.isDigit() } >= 8 -> amount
            amount == "0" -> k
            else -> amount + k
        }
    }

    fun pick(category: Category, food: FoodType?) {
        picked = category
        pickedFood = food
        val f = favIndex?.let { favorites.getOrNull(it) }
        // A favourite's split stays only while its own category is picked.
        if (f == null || f.category != category || f.foodType != food) {
            favIndex = null
            share = 100
        }
        error = null
    }

    fun record() {
        val cents = Money.parseToCents(amount)
        val category = picked
        error = when {
            cents == null || cents <= 0 -> "Type an amount first"
            category == null -> "Pick a category first"
            else -> null
        }
        if (error != null || category == null || cents == null) return
        onSave(
            Expense(
                id = UUID.randomUUID().toString(),
                date = day,
                title = title.trim().ifEmpty { Suggestions.defaultTitle(category, pickedFood) },
                amountCents = cents,
                category = category,
                myPercent = share,
                foodType = pickedFood,
            ),
        )
    }

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
                IconButton(onClick = ::record) { CheckGlyph("Record") }
            }
            if (favorites.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarGlyph(on = true, contentDescription = null, size = 18.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Favourites", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenFavorites) { Text("Edit ›") }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    favorites.forEachIndexed { i, f ->
                        FilterChip(
                            selected = favIndex == i,
                            onClick = {
                                if (favIndex == i) {
                                    favIndex = null
                                } else {
                                    favIndex = i
                                    title = f.title
                                    picked = f.category
                                    pickedFood = f.foodType
                                    share = f.myPercent
                                    error = null
                                }
                            },
                            label = { Text(f.title) },
                            leadingIcon = { ChipEmblem(f.foodType?.icon ?: f.category.icon) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            if (suggestions.isNotEmpty()) SuggestionList(suggestions) { s -> title = s.title }
            Text(
                if (greek) "${amount.ifEmpty { "0" }} €" else "€${amount.ifEmpty { "0" }}",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { pickingDate = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                EmblemBadge(Tab.CALENDAR.icon, size = 26.dp)
                Spacer(Modifier.width(8.dp))
                Text(day.format(dayFormat))
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
                error ?: picked?.let { c ->
                    (pickedFood?.label ?: c.label) + (if (share < 100) " · your share $share%" else "") + " · tap ✓ to record"
                } ?: "Pick a category, then tap ✓ at the top",
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            val tiles = Category.entries.map { Triple(it, null as FoodType?, it.icon to it.label) } +
                FoodType.entries.map { Triple(Category.ENTERTAINMENT, it as FoodType?, it.icon to it.label) }
            tiles.chunked(5).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { (category, food, look) ->
                        val glow = picked == category && pickedFood == food
                        Column(
                            Modifier
                                .weight(1f)
                                .background(
                                    if (glow) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else Color.Transparent,
                                    RoundedCornerShape(10.dp),
                                )
                                .clickable { pick(category, food) }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            EmblemBadge(look.first, size = 40.dp)
                            Spacer(Modifier.height(4.dp))
                            Text(look.second, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
                        }
                    }
                }
            }
            TextButton(
                onClick = { onMore(QuickPrefill(title, amount, day, picked, pickedFood, share.takeIf { it < 100 })) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("More details ›") }
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

/** Earlier titles matching what is typed, without amounts; tapping one calls [onPick]. */
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
                        s.foodType?.label ?: s.category.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
