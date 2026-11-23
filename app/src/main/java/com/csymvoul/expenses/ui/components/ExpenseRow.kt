package com.csymvoul.expenses.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.ui.theme.color
import com.csymvoul.expenses.ui.theme.icon
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val dayYearFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

/** One expense in a list. [showYear] for lists that span several years (search). */
@Composable
fun ExpenseRow(expense: Expense, onClick: () -> Unit, showYear: Boolean = false) {
    val details = buildString {
        append(expense.date.format(if (showYear) dayYearFormat else dayFormat))
        append(" · ").append(expense.category.label)
        expense.foodType?.let { append(" › ").append(it.label) }
        if (expense.isSplit) append(" · my share ${expense.myPercent}%")
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { EmblemBadge(expense.foodType?.icon ?: expense.category.icon) },
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
