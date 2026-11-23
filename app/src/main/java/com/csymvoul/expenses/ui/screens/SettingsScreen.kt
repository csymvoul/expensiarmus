package com.csymvoul.expenses.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.data.Settings
import com.csymvoul.expenses.data.ThemeMode
import com.csymvoul.expenses.domain.MoneyStyle
import com.csymvoul.expenses.ui.components.LocalWords
import com.csymvoul.expenses.ui.components.ParchmentCard
import com.csymvoul.expenses.ui.components.WizardTopBar
import java.time.LocalTime

/** Reminder on/off and time, wizard words, number format and theme. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    owlsBlocked: Boolean,
    onChange: ((Settings) -> Settings) -> Unit,
    onFixOwls: () -> Unit,
    onExport: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val w = LocalWords.current
    var pickingTime by remember { mutableStateOf(false) }
    val time = "%02d:%02d".format(settings.reminderTime.hour, settings.reminderTime.minute)
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = { WizardTopBar(title = w.settingsTab) },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ParchmentCard {
                Text(w.reminder, style = MaterialTheme.typography.titleMedium)
                SwitchRow(w.reminderSwitch, "A daily nudge to record the day's expenses", settings.reminderOn) { on ->
                    onChange { it.copy(reminderOn = on) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Time", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { pickingTime = true }, enabled = settings.reminderOn) { Text("🕘 $time") }
                }
                if (owlsBlocked && settings.reminderOn) {
                    TextButton(onClick = onFixOwls) {
                        Text("Notifications are off for the app: tap to allow them", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            ParchmentCard {
                Text("Wizard words", style = MaterialTheme.typography.titleMedium)
                SwitchRow("Use wizard words", "Protego, Accio, Evanesco… Off: Goals, Search, Delete.", settings.wizardWords) { on ->
                    onChange { it.copy(wizardWords = on) }
                }
            }
            ParchmentCard {
                Text("Number format", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(settings.moneyStyle == MoneyStyle.ENGLISH, { onChange { it.copy(moneyStyle = MoneyStyle.ENGLISH) } }, { Text("€1,234.56") })
                    FilterChip(settings.moneyStyle == MoneyStyle.GREEK, { onChange { it.copy(moneyStyle = MoneyStyle.GREEK) } }, { Text("1.234,56 €") })
                }
            }
            ParchmentCard {
                Text(w.exportTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    "All your expenses as a CSV file, for a backup or a spreadsheet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onExport, shape = CutCornerShape(10.dp)) { Text(w.exportButton) }
            }
            ParchmentCard {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ThemeMode.SYSTEM to "📱 Like the phone", ThemeMode.LIGHT to "☀️ Light", ThemeMode.DARK to "🌙 Dark").forEach { (mode, label) ->
                        FilterChip(settings.theme == mode, { onChange { it.copy(theme = mode) } }, { Text(label) })
                    }
                }
            }
        }
    }

    if (pickingTime) {
        val state = rememberTimePickerState(settings.reminderTime.hour, settings.reminderTime.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text("Reminder time") },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    onChange { it.copy(reminderTime = LocalTime.of(state.hour, state.minute)) }
                    pickingTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SwitchRow(title: String, note: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
