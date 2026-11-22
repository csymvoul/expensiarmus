package com.csymvoul.expenses

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.csymvoul.expenses.domain.ExpenseQueries
import com.csymvoul.expenses.domain.MonthlySummary
import com.csymvoul.expenses.domain.PeriodSummary
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.reminder.OwlNotifier
import com.csymvoul.expenses.reminder.ReminderScheduler
import com.csymvoul.expenses.ui.ExpenseViewModel
import com.csymvoul.expenses.ui.screens.AnnalsScreen
import com.csymvoul.expenses.ui.screens.AnnalsView
import com.csymvoul.expenses.ui.screens.CategoryScreen
import com.csymvoul.expenses.ui.screens.DayDetails
import com.csymvoul.expenses.ui.screens.EditExpenseScreen
import com.csymvoul.expenses.ui.screens.GoalsScreen
import com.csymvoul.expenses.ui.screens.MonthScreen
import com.csymvoul.expenses.ui.screens.MonthView
import com.csymvoul.expenses.ui.screens.SearchScreen
import com.csymvoul.expenses.ui.components.WizardBackdrop
import com.csymvoul.expenses.ui.theme.ExpensesTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReminderScheduler.ensureChannel(this)
        ReminderScheduler.schedule(this)
        val record = intent.getBooleanExtra(EXTRA_RECORD, false)
        setContent {
            ExpensesTheme { ExpensesApp(openRecord = record) }
        }
    }

    companion object {
        /** Set by the reminder notification: open straight on "Record expense". */
        const val EXTRA_RECORD = "record_expense"
    }
}

// Routes. The back stack is kept as routes joined by '|' so it survives rotation.
private const val MONTH = "month"
private const val GOALS = "goals"
private const val SEARCH = "search"
private const val ANNALS = "annals"
private const val EDIT = "edit:" // + expense id, empty for a new expense
private const val CATEGORY = "category:" // + category name, optionally + ":" + food type name

@Composable
fun ExpensesApp(openRecord: Boolean = false, vm: ExpenseViewModel = viewModel()) {
    val data by vm.state.collectAsStateWithLifecycle()
    var stack by rememberSaveable { mutableStateOf(if (openRecord) "$MONTH|$EDIT" else MONTH) }
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var view by rememberSaveable { mutableStateOf(MonthView.OVERVIEW) }
    var selectedDay by rememberSaveable { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var annalsView by rememberSaveable { mutableStateOf(AnnalsView.YEAR) }
    var annalsYear by rememberSaveable { mutableStateOf(LocalDate.now().year) }
    val route = stack.substringAfterLast('|')

    fun push(r: String) { stack = "$stack|$r" }
    fun replaceTop(r: String) { stack = stack.substringBeforeLast('|', "") .let { if (it.isEmpty()) r else "$it|$r" } }
    fun pop() { stack = stack.substringBeforeLast('|', MONTH) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = vm.exportCsv(context.contentResolver, uri)
                Toast.makeText(context, if (ok) "Geminio! Ledger copied to CSV." else "The spell failed: export did not work.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var reminderOn by remember { mutableStateOf(ReminderScheduler.isEnabled(context)) }
    var owlsBlocked by remember { mutableStateOf(!OwlNotifier.canNotify(context)) }
    // Re-check whenever the app comes back, e.g. from the notification settings.
    LifecycleResumeEffect(Unit) {
        owlsBlocked = !OwlNotifier.canNotify(context)
        onPauseOrDispose {}
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        owlsBlocked = !OwlNotifier.canNotify(context)
    }
    fun askForNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LaunchedEffect(Unit) {
        if (reminderOn && ReminderScheduler.firstPermissionAsk(context)) askForNotifications()
    }

    BackHandler(enabled = route != MONTH) { pop() }

    WizardBackdrop {
        when {
            route == GOALS -> GoalsScreen(
                goals = data.goals,
                onSave = { vm.setGoals(it); pop() },
                onBack = ::pop,
            )
            route == ANNALS -> {
                val today = LocalDate.now()
                val period = remember(data, annalsView, annalsYear) {
                    if (annalsView == AnnalsView.YEAR) PeriodSummary.year(data, annalsYear, today) else PeriodSummary.allTime(data, today)
                }
                AnnalsScreen(
                    summary = period,
                    view = annalsView,
                    year = annalsYear,
                    today = today,
                    onViewChange = { annalsView = it },
                    onPreviousYear = { annalsYear-- },
                    onNextYear = { annalsYear++ },
                    onOpenMonth = {
                        month = it
                        view = MonthView.OVERVIEW
                        stack = MONTH
                    },
                    onOpenYear = {
                        annalsYear = it
                        annalsView = AnnalsView.YEAR
                    },
                    onCategory = { c, t -> push(CATEGORY + c.name + (t?.let { ":" + it.name } ?: "")) },
                    onBack = ::pop,
                )
            }
            route == SEARCH -> SearchScreen(
                expenses = data.expenses,
                onOpen = { push(EDIT + it) },
                onBack = ::pop,
            )
            route.startsWith(CATEGORY) -> {
                val parts = route.removePrefix(CATEGORY).split(':')
                val category = Category.valueOf(parts[0])
                val foodType = parts.getOrNull(1)?.let { FoodType.valueOf(it) }
                val groups = remember(data, category, foodType) {
                    ExpenseQueries.byCategory(data.expenses, category, foodType)
                }
                CategoryScreen(
                    category = category,
                    foodType = foodType,
                    groups = groups,
                    onFoodType = { t -> replaceTop(CATEGORY + category.name + (t?.let { ":" + it.name } ?: "")) },
                    onOpen = { push(EDIT + it) },
                    onBack = ::pop,
                )
            }
            route.startsWith(EDIT) -> key(route) {
                val existing = route.removePrefix(EDIT).ifEmpty { null }?.let(data::find)
                EditExpenseScreen(
                    existing = existing,
                    defaultDate = when {
                        view == MonthView.CALENDAR && selectedDay?.let { YearMonth.from(it) } == month -> selectedDay!!
                        month == YearMonth.now() -> LocalDate.now()
                        else -> month.atDay(1)
                    },
                    onSave = {
                        vm.upsert(it)
                        month = YearMonth.from(it.date)
                        pop()
                    },
                    onDelete = { vm.delete(it); pop() },
                    onBack = ::pop,
                )
            }
            else -> {
                val summary = remember(data, month) { MonthlySummary.of(data, month) }
                val day = selectedDay?.takeIf { YearMonth.from(it) == month }
                val days = remember(data, month, day) {
                    DayDetails(
                        totals = ExpenseQueries.dailyTotals(data.expenses, month),
                        selected = day,
                        expenses = day?.let { ExpenseQueries.onDay(data.expenses, it) }.orEmpty(),
                    )
                }
                MonthScreen(
                    summary = summary,
                    view = view,
                    onViewChange = { view = it },
                    days = days,
                    onSelectDay = { selectedDay = it },
                    onPreviousMonth = { month = month.minusMonths(1) },
                    onNextMonth = { month = month.plusMonths(1) },
                    onAdd = { push(EDIT) },
                    onOpen = { push(EDIT + it) },
                    onCategory = { c, t -> push(CATEGORY + c.name + (t?.let { ":" + it.name } ?: "")) },
                    onSearch = { push(SEARCH) },
                    onAnnals = {
                        annalsYear = month.year
                        annalsView = AnnalsView.YEAR
                        push(ANNALS)
                    },
                    onGoals = { push(GOALS) },
                    reminderOn = reminderOn,
                    owlsBlocked = owlsBlocked,
                    onFixOwls = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    },
                    onTestOwl = {
                        if (!OwlNotifier.show(context)) {
                            Toast.makeText(context, "Owl reminders are blocked: allow notifications first.", Toast.LENGTH_LONG).show()
                            askForNotifications()
                        }
                    },
                    onToggleReminder = {
                        reminderOn = !reminderOn
                        ReminderScheduler.setEnabled(context, reminderOn)
                        if (reminderOn) askForNotifications()
                    },
                    onExport = { exportLauncher.launch("expensiarmus-${LocalDate.now()}.csv") },
                )
            }
        }
    }
}
