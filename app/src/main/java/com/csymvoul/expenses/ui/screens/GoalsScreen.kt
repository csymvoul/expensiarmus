package com.csymvoul.expenses.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Goals
import com.csymvoul.expenses.ui.components.WizardTopBar
import com.csymvoul.expenses.ui.theme.emoji

/** Blank means "no goal"; anything else must be a positive amount. Returns false when invalid. */
private fun goalValid(text: String): Boolean =
    text.isBlank() || (Money.parseToCents(text) ?: 0L) > 0

private fun goalCents(text: String): Long? = if (text.isBlank()) null else Money.parseToCents(text)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(goals: Goals, onSave: (Goals) -> Unit, onBack: () -> Unit) {
    var overall by rememberSaveable { mutableStateOf(goals.monthlyCents?.let(Money::forInput) ?: "") }
    val perCategory: Map<Category, MutableState<String>> = Goals.CATEGORIES_WITH_GOALS.associateWith { c ->
        rememberSaveable(c.name) { mutableStateOf(goals.perCategoryCents[c]?.let(Money::forInput) ?: "") }
    }
    val allValid = goalValid(overall) && perCategory.values.all { goalValid(it.value) }

    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground, // a transparent scaffold would otherwise default to black text
        topBar = {
            WizardTopBar(
                title = "Protego · Goals",
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Protego! Cast a ward on your spending. Goals apply to every month until you change them; leave a field empty for no goal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GoalField("Total per month", overall) { overall = it }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text("A ward for Revelry & Feasts", style = MaterialTheme.typography.titleSmall)
            Goals.CATEGORIES_WITH_GOALS.forEach { c ->
                val state = perCategory.getValue(c)
                GoalField("${c.emoji} ${c.label}", state.value) { state.value = it }
            }
            val categorySum = perCategory.values.mapNotNull { goalCents(it.value) }.sum()
            val overallCents = goalCents(overall)
            if (allValid && overallCents != null && categorySum > overallCents) {
                Text(
                    "The Entertainment goal (${Money.format(categorySum)}) is more than the monthly total.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = {
                    onSave(
                        Goals(
                            monthlyCents = goalCents(overall),
                            perCategoryCents = perCategory
                                .mapNotNull { (c, s) -> goalCents(s.value)?.let { c to it } }
                                .toMap(),
                        ),
                    )
                },
                enabled = allValid,
                shape = CutCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("🛡 Cast the wards · Save") }
        }
    }
}

@Composable
private fun GoalField(label: String, value: String, onChange: (String) -> Unit) {
    val valid = goalValid(value)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        prefix = { Text("€ ") },
        singleLine = true,
        isError = !valid,
        supportingText = if (!valid) ({ Text("Enter an amount, or leave empty") }) else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
    )
}
