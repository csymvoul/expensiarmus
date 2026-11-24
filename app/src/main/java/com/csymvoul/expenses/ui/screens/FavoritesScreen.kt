package com.csymvoul.expenses.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.domain.Favorites
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Favorite
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.ui.components.ChipEmblem
import com.csymvoul.expenses.ui.components.EmblemBadge
import com.csymvoul.expenses.ui.components.LocalWords
import com.csymvoul.expenses.ui.components.ParchmentCard
import com.csymvoul.expenses.ui.components.StarGlyph
import com.csymvoul.expenses.ui.components.WizardButton
import com.csymvoul.expenses.ui.components.WizardTopBar
import com.csymvoul.expenses.ui.theme.icon

private val SHARES = listOf(100 to "Not split", 50 to "50-50", 33 to "33%", 25 to "25%")

/**
 * Favourites: up to five, each editable (title, category, split) or removable, and new ones added
 * here too. [onSave] returns what to fix, or null when saved.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoritesScreen(
    favorites: List<Favorite>,
    onSave: (index: Int?, Favorite) -> String?,
    onRemove: (index: Int) -> Unit,
    onBack: () -> Unit,
) {
    val w = LocalWords.current
    // -1: nothing open; -2: a new favourite; otherwise the index being edited.
    var open by rememberSaveable { mutableStateOf(-1) }
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            WizardTopBar(
                title = "Favourites",
                glyph = { StarGlyph(on = true, contentDescription = null, size = 32.dp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Up to ${Favorites.MAX}. In ${w.newExpense}, tap one to fill in its title, category and split, then type the amount. You can also star any expense on its page.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (favorites.isEmpty() && open == -1) {
                Text("No favourites yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
            }
            favorites.forEachIndexed { i, f ->
                ParchmentCard {
                    if (open == i) {
                        FavoriteEditor(f, onSave = { onSave(i, it).also { err -> if (err == null) open = -1 } }, onRemove = { onRemove(i); open = -1 }, onCancel = { open = -1 })
                    } else {
                        Row(Modifier.fillMaxWidth().clickable { open = i }, verticalAlignment = Alignment.CenterVertically) {
                            EmblemBadge(f.foodType?.icon ?: f.category.icon, size = 36.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    (f.foodType?.label ?: f.category.label) + if (f.myPercent < 100) " · your share ${f.myPercent}%" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        }
                    }
                }
            }
            if (open == -2) {
                ParchmentCard {
                    Text("A new favourite", style = MaterialTheme.typography.titleMedium)
                    FavoriteEditor(
                        Favorite("", Category.SUPERMARKET),
                        onSave = { onSave(null, it).also { err -> if (err == null) open = -1 } },
                        onRemove = null,
                        onCancel = { open = -1 },
                    )
                }
            } else if (open == -1 && favorites.size < Favorites.MAX) {
                WizardButton(onClick = { open = -2 }, modifier = Modifier.fillMaxWidth()) {
                    StarGlyph(on = true, contentDescription = null, size = 24.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Add a favourite")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FavoriteEditor(start: Favorite, onSave: (Favorite) -> String?, onRemove: (() -> Unit)?, onCancel: () -> Unit) {
    var title by rememberSaveable { mutableStateOf(start.title) }
    var category by rememberSaveable { mutableStateOf(start.category) }
    var food by rememberSaveable { mutableStateOf(start.foodType) }
    var share by rememberSaveable { mutableStateOf(start.myPercent) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Category", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Category.entries.forEach { c ->
                FilterChip(
                    selected = category == c,
                    onClick = { category = c; if (c != Category.ENTERTAINMENT) food = null },
                    label = { Text(c.label) },
                    leadingIcon = { ChipEmblem(c.icon) },
                )
            }
        }
        if (category == Category.ENTERTAINMENT) {
            Text("Is it food?", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = food == null, onClick = { food = null }, label = { Text("Not food") })
                FoodType.entries.forEach { t ->
                    FilterChip(selected = food == t, onClick = { food = t }, label = { Text(t.label) }, leadingIcon = { ChipEmblem(t.icon) })
                }
            }
        }
        Text("Your share", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val shares = if (SHARES.any { it.first == share }) SHARES else SHARES + (share to "$share%")
            shares.forEach { (p, label) -> FilterChip(selected = share == p, onClick = { share = p }, label = { Text(label) }) }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onRemove != null) TextButton(onClick = onRemove) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("Cancel") }
            WizardButton(
                onClick = { error = onSave(Favorite(title, category, food, share)) },
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            ) { Text("Save") }
        }
    }
}
