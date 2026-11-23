package com.csymvoul.expenses.ui.screens

import com.csymvoul.expenses.ui.components.ChipEmblem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.domain.MonthGroup
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.ui.components.EmblemBadge
import com.csymvoul.expenses.ui.components.WizardTopBar
import com.csymvoul.expenses.ui.components.ExpenseRow
import com.csymvoul.expenses.ui.components.LocalWords
import com.csymvoul.expenses.ui.components.ParchmentCard
import com.csymvoul.expenses.ui.theme.color
import com.csymvoul.expenses.ui.theme.emoji
import com.csymvoul.expenses.ui.theme.icon
import com.csymvoul.expenses.ui.theme.nickname
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

/** Every expense of one category, grouped by month, newest first. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryScreen(
    category: Category,
    foodType: FoodType?,
    groups: List<MonthGroup>,
    onFoodType: (FoodType?) -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground, // a transparent scaffold would otherwise default to black text
        topBar = {
            WizardTopBar(
                title = "${category.emoji} ${category.label}",
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
                ParchmentCard(spacing = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmblemBadge(category.icon, size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            if (LocalWords.current.nicknames) {
                                Text(
                                    category.nickname,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontStyle = FontStyle.Italic,
                                )
                            }
                            Text(category.hint, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Text(
                        if (foodType == null) "All time" else "All time · ${foodType.label}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        Money.format(groups.sumOf { it.totalCents }),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    val count = groups.sumOf { it.expenses.size }
                    Text(
                        "${if (count == 1) "1 expense" else "$count expenses"} in " +
                            if (groups.size == 1) "1 month" else "${groups.size} months",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (category == Category.ENTERTAINMENT) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = foodType == null, onClick = { onFoodType(null) }, label = { Text("All") })
                        FoodType.entries.forEach { t ->
                            FilterChip(
                                selected = foodType == t,
                                onClick = { onFoodType(t) },
                                label = { Text(t.label) },
                                leadingIcon = { ChipEmblem(t.icon) },
                            )
                        }
                    }
                }
            }
            if (groups.isEmpty()) {
                item {
                    Text(
                        LocalWords.current.categoryEmpty,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }
            groups.forEach { g ->
                item(key = "month-${g.month}") {
                    Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            g.month.format(monthFormat),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(Money.format(g.totalCents), fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                }
                items(g.expenses, key = { it.id }) { expense ->
                    ExpenseRow(expense, onClick = { onOpen(expense.id) })
                }
            }
        }
    }
}
