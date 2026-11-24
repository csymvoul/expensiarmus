package com.csymvoul.expenses.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.domain.CsvImporter
import com.csymvoul.expenses.domain.ImportResult
import com.csymvoul.expenses.ui.components.LocalWords

/** Apparate's confirmation: what the file holds, what is new, and which rows could not be read. */
@Composable
fun ImportDialog(result: ImportResult, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val w = LocalWords.current
    val adding = result.added.size
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(w.importTitle) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CsvImporter.summary(result).forEach { line ->
                    Text(
                        line,
                        style = if (line.startsWith("Line ") || line.startsWith("…")) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        color = if (line.startsWith("Line ")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = if (adding > 0) onConfirm else onDismiss) {
                Text(if (adding == 0) "OK" else if (adding == 1) "Add 1 expense" else "Add $adding expenses")
            }
        },
        dismissButton = if (adding > 0) ({ TextButton(onClick = onDismiss) { Text("Cancel") } }) else null,
    )
}
