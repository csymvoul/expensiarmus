package com.csymvoul.expenses

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.csymvoul.expenses.domain.MonthlySummary
import com.csymvoul.expenses.ui.ExpenseViewModel
import com.csymvoul.expenses.ui.screens.EditExpenseScreen
import com.csymvoul.expenses.ui.screens.GoalsScreen
import com.csymvoul.expenses.ui.screens.MonthScreen
import com.csymvoul.expenses.ui.theme.ExpensesTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExpensesTheme { ExpensesApp() }
        }
    }
}

private const val MONTH = "month"
private const val GOALS = "goals"
private const val EDIT = "edit:" // followed by the expense id, empty for a new expense

@Composable
fun ExpensesApp(vm: ExpenseViewModel = viewModel()) {
    val data by vm.state.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf(MONTH) }
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = vm.exportCsv(context.contentResolver, uri)
                Toast.makeText(context, if (ok) "Expenses exported" else "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = route != MONTH) { route = MONTH }

    when {
        route == GOALS -> GoalsScreen(
            goals = data.goals,
            onSave = { vm.setGoals(it); route = MONTH },
            onBack = { route = MONTH },
        )
        route.startsWith(EDIT) -> key(route) {
            val existing = route.removePrefix(EDIT).ifEmpty { null }?.let(data::find)
            EditExpenseScreen(
                existing = existing,
                defaultDate = if (month == YearMonth.now()) LocalDate.now() else month.atDay(1),
                onSave = {
                    vm.upsert(it)
                    month = YearMonth.from(it.date)
                    route = MONTH
                },
                onDelete = { vm.delete(it); route = MONTH },
                onBack = { route = MONTH },
            )
        }
        else -> {
            val summary = remember(data, month) { MonthlySummary.of(data, month) }
            MonthScreen(
                summary = summary,
                onPreviousMonth = { month = month.minusMonths(1) },
                onNextMonth = { month = month.plusMonths(1) },
                onAdd = { route = EDIT },
                onOpen = { route = EDIT + it },
                onGoals = { route = GOALS },
                onExport = { exportLauncher.launch("expenses-${LocalDate.now()}.csv") },
            )
        }
    }
}
