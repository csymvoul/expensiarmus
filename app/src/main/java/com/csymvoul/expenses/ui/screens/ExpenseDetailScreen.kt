package com.csymvoul.expenses.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.domain.ExpenseDetails
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.ui.components.EmblemBadge
import com.csymvoul.expenses.ui.components.StarGlyph
import com.csymvoul.expenses.ui.components.LocalWords
import com.csymvoul.expenses.ui.components.ParchmentCard
import com.csymvoul.expenses.ui.components.WizardButton
import com.csymvoul.expenses.ui.components.WizardTopBar
import com.csymvoul.expenses.ui.theme.color
import com.csymvoul.expenses.ui.theme.emoji
import com.csymvoul.expenses.ui.theme.icon
import com.csymvoul.expenses.ui.components.Tab
import com.csymvoul.expenses.ui.theme.nickname
import java.time.format.DateTimeFormatter
import java.util.Locale

private val longDate = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH)

/** Read-only view of one expense. Amend and Evanesco (delete) are the buttons at the bottom; the top bar only has Back. */
@Composable
fun ExpenseDetailScreen(
    expense: Expense?,
    /** Whether this expense's title, category and food type are a favourite. */
    isFavorite: Boolean,
    onToggleFavorite: (Expense) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
) {
    // Deleted from the edit screen, or gone: there is nothing to show.
    if (expense == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val w = LocalWords.current
    var confirmingDelete by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            WizardTopBar(
                title = w.entry,
                icon = Tab.LEDGER.icon,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ParchmentCard(spacing = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmblemBadge(expense.foodType?.icon ?: expense.category.icon, size = 52.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(expense.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            Money.format(expense.myShareCents),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (expense.isSplit) {
                            Text(
                                "of ${Money.format(expense.amountCents)} in full",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    // The star: a favourite keeps the title, category and split for Quick add.
                    IconButton(onClick = { onToggleFavorite(expense) }, modifier = Modifier.align(Alignment.Top)) {
                        StarGlyph(on = isFavorite, contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites", size = 32.dp)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                DetailRow("Date", expense.date.format(longDate))
                DetailRow("Category", "${expense.category.emoji} ${expense.category.label}", expense.category.nickname.takeIf { w.nicknames })
                expense.foodType?.let { DetailRow("Food", "${it.emoji} ${it.label}", it.nickname.takeIf { w.nicknames }) }
                DetailRow("Split", ExpenseDetails.splitLine(expense))
            }
            WizardButton(onClick = { onEdit(expense.id) }, modifier = Modifier.fillMaxWidth()) {
                Text(w.amend)
            }
            TextButton(onClick = { confirmingDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(w.deleteLink, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(w.deleteTitle) },
            text = { Text(w.deleteBody(expense.title)) },
            confirmButton = {
                TextButton(onClick = { confirmingDelete = false; onDelete(expense.id) }) { Text(w.deleteBtn) }
            },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text(w.keep) } },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, note: String? = null) {
    Column {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Text(value, style = MaterialTheme.typography.bodyLarge)
        note?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
