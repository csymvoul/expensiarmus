package com.csymvoul.expenses.ui.screens

import com.csymvoul.expenses.ui.components.ChipEmblem
import com.csymvoul.expenses.ui.components.SearchGlyph

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.domain.ExpenseQueries
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.ui.components.WizardTopBar
import com.csymvoul.expenses.ui.components.ExpenseRow
import com.csymvoul.expenses.ui.components.LocalWords
import com.csymvoul.expenses.ui.theme.icon

/** Search all expenses by title, category or food type, optionally narrowed to some categories. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(expenses: List<Expense>, onOpen: (String) -> Unit, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    // Selected category names, comma separated, so the selection survives rotation.
    var picked by rememberSaveable { mutableStateOf("") }
    val categories = picked.split(',').filter { it.isNotEmpty() }.map { Category.valueOf(it) }.toSet()
    val results = remember(expenses, query, picked) { ExpenseQueries.search(expenses, query, categories) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground, // a transparent scaffold would otherwise default to black text
        topBar = {
            WizardTopBar(
                title = LocalWords.current.search,
                glyph = { SearchGlyph() },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(LocalWords.current.searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Category.entries.forEach { c ->
                        val selected = c in categories
                        FilterChip(
                            selected = selected,
                            onClick = {
                                picked = (if (selected) categories - c else categories + c)
                                    .sortedBy { it.ordinal }
                                    .joinToString(",") { it.name }
                            },
                            label = { Text(c.label) },
                            leadingIcon = { ChipEmblem(c.icon) },
                        )
                    }
                }
            }
            if (query.isBlank() && categories.isEmpty()) {
                item { SearchHint(LocalWords.current.searchHint) }
            } else {
                item {
                    Text(
                        "${if (results.size == 1) "1 expense" else "${results.size} expenses"} · " +
                            Money.format(results.sumOf { it.myShareCents }),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (results.isEmpty()) {
                    item { SearchHint(LocalWords.current.searchNothing) }
                }
                items(results, key = { it.id }) { expense ->
                    ExpenseRow(expense, onClick = { onOpen(expense.id) }, showYear = true)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun SearchHint(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    )
}
