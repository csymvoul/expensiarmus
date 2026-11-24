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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import java.io.File
import com.csymvoul.expenses.domain.Release
import com.csymvoul.expenses.update.UpdateState
import com.csymvoul.expenses.domain.MoneyStyle
import com.csymvoul.expenses.ui.components.LocalWords
import com.csymvoul.expenses.domain.Words
import com.csymvoul.expenses.R
import com.csymvoul.expenses.ui.components.Tab
import com.csymvoul.expenses.ui.components.ChipEmblem
import com.csymvoul.expenses.ui.components.EmblemBadge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import com.csymvoul.expenses.ui.components.ParchmentCard
import com.csymvoul.expenses.ui.components.WizardButton
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
    onImport: () -> Unit,
    updateState: UpdateState,
    appVersion: String,
    canInstall: Boolean,
    onCheckUpdates: () -> Unit,
    onDownloadUpdate: (Release) -> Unit,
    onInstallUpdate: (File) -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    val w = LocalWords.current
    var pickingTime by remember { mutableStateOf(false) }
    val time = "%02d:%02d".format(settings.reminderTime.hour, settings.reminderTime.minute)
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = { WizardTopBar(title = w.settingsTab, icon = Tab.SETTINGS.icon) },
        bottomBar = bottomBar,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ParchmentCard {
                CardTitle(R.drawable.emblem_set_reminder, w.reminder)
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
                CardTitle(R.drawable.emblem_set_geminio, w.exportTitle)
                Text(
                    "All your expenses as a CSV file, for a backup or a spreadsheet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WizardButton(onClick = onExport) { Text(w.exportButton) }
            }
            ParchmentCard {
                CardTitle(R.drawable.emblem_set_apparate, w.importTitle)
                Text(
                    "Bring expenses in from a CSV: a Geminio backup, or a spreadsheet with the same columns. " +
                        "You see what will be added first; expenses already in the ledger are skipped.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                WizardButton(onClick = onImport) { Text(w.importButton) }
            }
            UpdatesCard(settings, updateState, appVersion, canInstall, onChange, onCheckUpdates, onDownloadUpdate, onInstallUpdate)
            ParchmentCard {
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple(ThemeMode.SYSTEM, "Like the phone", R.drawable.emblem_set_theme_system),
                        Triple(ThemeMode.LIGHT, "Light", R.drawable.emblem_set_theme_light),
                        Triple(ThemeMode.DARK, "Dark", R.drawable.emblem_set_theme_dark),
                    ).forEach { (mode, label, icon) ->
                        FilterChip(
                            settings.theme == mode,
                            { onChange { it.copy(theme = mode) } },
                            { Text(label) },
                            leadingIcon = { ChipEmblem(icon) },
                        )
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

/** Settings → Updates: the daily check switch, this version, and what to do about a newer one. */
@Composable
private fun UpdatesCard(
    settings: Settings,
    state: UpdateState,
    appVersion: String,
    canInstall: Boolean,
    onChange: ((Settings) -> Settings) -> Unit,
    onCheck: () -> Unit,
    onDownload: (Release) -> Unit,
    onInstall: (File) -> Unit,
) {
    ParchmentCard {
        Text("Updates", style = MaterialTheme.typography.titleMedium)
        SwitchRow(
            "Check for new versions",
            "Once a day the app asks GitHub whether a newer Expensiarmus is out, and sends a notification if so. Nothing about your expenses is sent.",
            settings.updateCheck,
        ) { on -> onChange { it.copy(updateCheck = on) } }
        Text("This is version $appVersion.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when (state) {
            UpdateState.Idle -> OutlinedButton(onClick = onCheck) { Text("Check now") }
            UpdateState.Checking -> Text("Asking GitHub…", style = MaterialTheme.typography.bodyMedium)
            UpdateState.UpToDate -> {
                Text("You have the latest version.", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = onCheck) { Text("Check again") }
            }
            is UpdateState.Available -> {
                Text("Expensiarmus ${state.release.version} is out.", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                WizardButton(onClick = { onDownload(state.release) }) { Text("Download and install") }
            }
            is UpdateState.Downloading -> {
                Text("Downloading ${state.release.version}… ${state.percent}%", style = MaterialTheme.typography.bodyMedium)
                LinearProgressIndicator(progress = { state.percent / 100f }, modifier = Modifier.fillMaxWidth())
            }
            is UpdateState.ReadyToInstall -> {
                Text(
                    if (canInstall) "Downloaded. Tap Install, then Update in Android's installer. Your expenses stay as they are."
                    else "Downloaded. Android will first ask you to allow installs from Expensiarmus (only once); then come back and tap Install.",
                    style = MaterialTheme.typography.bodySmall,
                )
                WizardButton(onClick = { onInstall(state.file) }) { Text("Install ${state.release.version}") }
            }
            is UpdateState.Failed -> {
                Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                OutlinedButton(onClick = { state.release?.let(onDownload) ?: onCheck() }) { Text("Try again") }
            }
        }
    }
}

@Composable
private fun CardTitle(@DrawableRes icon: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EmblemBadge(icon, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Text(Words.withoutEmoji(title), style = MaterialTheme.typography.titleMedium)
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
