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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.csymvoul.expenses.data.AppSettings
import com.csymvoul.expenses.domain.ExpenseQueries
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.domain.MonthlySummary
import com.csymvoul.expenses.domain.PeriodSummary
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.FoodType
import com.csymvoul.expenses.reminder.OwlNotifier
import com.csymvoul.expenses.reminder.ReminderScheduler
import com.csymvoul.expenses.ui.ExpenseViewModel
import com.csymvoul.expenses.ui.components.LocalWords
import com.csymvoul.expenses.ui.components.Tab
import com.csymvoul.expenses.ui.components.WizardBackdrop
import com.csymvoul.expenses.ui.components.WizardTabBar
import com.csymvoul.expenses.ui.screens.AnnalsScreen
import com.csymvoul.expenses.ui.screens.AnnalsView
import com.csymvoul.expenses.ui.screens.CategoryScreen
import com.csymvoul.expenses.ui.screens.DayDetails
import com.csymvoul.expenses.ui.screens.EditExpenseScreen
import com.csymvoul.expenses.ui.screens.ExpenseDetailScreen
import com.csymvoul.expenses.ui.screens.GoalsScreen
import com.csymvoul.expenses.ui.screens.MonthScreen
import com.csymvoul.expenses.ui.screens.MonthView
import com.csymvoul.expenses.ui.screens.QuickAddSheet
import com.csymvoul.expenses.ui.screens.QuickPrefill
import com.csymvoul.expenses.ui.screens.SearchScreen
import com.csymvoul.expenses.ui.screens.SettingsScreen
import com.csymvoul.expenses.ui.theme.ExpensesTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppSettings.get(this)
        ReminderScheduler.ensureChannel(this)
        ReminderScheduler.schedule(this)
        val record = intent.getBooleanExtra(EXTRA_RECORD, false)
        setContent {
            val settings by AppSettings.state.collectAsState()
            ExpensesTheme(settings.theme) {
                // Static locals: any change of settings (words, number style) redraws every screen.
                CompositionLocalProvider(LocalWords provides settings.words, LocalSettingsVersion provides settings) {
                    ExpensesApp(openRecord = record)
                }
            }
        }
    }

    companion object {
        /** Set by the reminder notification and the widget's ➕: open Quick add. */
        const val EXTRA_RECORD = "record_expense"
    }
}

private val LocalSettingsVersion = staticCompositionLocalOf<Any> { Unit }

// Routes. The back stack is kept as routes joined by '|' so it survives rotation.
// The five tabs (Ledger, Calendar, Annals, Protego, Settings) are top-level routes.
private const val MONTH = "month"
private const val CALENDAR = "calendar"
private const val ANNALS = "annals"
private const val GOALS = "goals"
private const val SETTINGS = "settings"
private const val SEARCH = "search"
private const val EDIT = "edit:" // + expense id, empty for a new expense
private const val VIEW = "view:" // + expense id: read-only view
private const val CATEGORY = "category:" // + category name, optionally + ":" + food type name

private val TAB_ROUTES = mapOf(Tab.LEDGER to MONTH, Tab.CALENDAR to CALENDAR, Tab.ANNALS to ANNALS, Tab.GOALS to GOALS, Tab.SETTINGS to SETTINGS)

@Composable
fun ExpensesApp(openRecord: Boolean = false, vm: ExpenseViewModel = viewModel()) {
    val data by vm.state.collectAsStateWithLifecycle()
    val settings by AppSettings.state.collectAsStateWithLifecycle()
    var stack by rememberSaveable { mutableStateOf(MONTH) }
    var month by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var selectedDay by rememberSaveable { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var annalsView by rememberSaveable { mutableStateOf(AnnalsView.YEAR) }
    var annalsYear by rememberSaveable { mutableStateOf(LocalDate.now().year) }
    var quickOpen by rememberSaveable { mutableStateOf(openRecord) }
    var prefill by remember { mutableStateOf<QuickPrefill?>(null) }
    val route = stack.substringAfterLast('|')

    fun push(r: String) { stack = "$stack|$r" }
    fun replaceTop(r: String) { stack = stack.substringBeforeLast('|', "") .let { if (it.isEmpty()) r else "$it|$r" } }
    fun pop() { stack = stack.substringBeforeLast('|', MONTH) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val words = LocalWords.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = vm.exportCsv(context.contentResolver, uri)
                Toast.makeText(context, if (ok) words.exported else words.exportFailed, Toast.LENGTH_SHORT).show()
            }
        }
    }

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
        if (settings.reminderOn && ReminderScheduler.firstPermissionAsk(context)) askForNotifications()
    }
    val fixOwls = {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
        )
    }
    val newExpenseDate = when {
        route == CALENDAR && selectedDay?.let { YearMonth.from(it) } == month -> selectedDay!!
        month == YearMonth.now() -> LocalDate.now()
        else -> month.atDay(1)
    }
    val tabBar: @Composable (Tab) -> Unit = { selected ->
        WizardTabBar(selected) { tab -> stack = TAB_ROUTES.getValue(tab) }
    }

    BackHandler(enabled = route != MONTH) { pop() }

    WizardBackdrop {
        when {
            route == GOALS -> GoalsScreen(
                month = month,
                goals = data.goals,
                spent = remember(data, month) {
                    MonthlySummary.of(data, month).categories.associate { it.category to it.spentCents }
                },
                onSet = { c, cents, onwards -> vm.setGoal(c, month, cents, onwards) },
                onRemove = { c, onwards -> vm.removeGoal(c, month, onwards) },
                onPreviousMonth = { month = month.minusMonths(1) },
                onNextMonth = { month = month.plusMonths(1) },
                bottomBar = { tabBar(Tab.GOALS) },
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
                        stack = MONTH
                    },
                    onOpenYear = {
                        annalsYear = it
                        annalsView = AnnalsView.YEAR
                    },
                    onCategory = { c, t -> push(CATEGORY + c.name + (t?.let { ":" + it.name } ?: "")) },
                    bottomBar = { tabBar(Tab.ANNALS) },
                )
            }
            route == SETTINGS -> SettingsScreen(
                settings = settings,
                owlsBlocked = owlsBlocked,
                onChange = { change ->
                    val before = settings
                    AppSettings.update(context, change)
                    ReminderScheduler.reschedule(context)
                    vm.refreshWidget()
                    if (!before.reminderOn && AppSettings.get(context).reminderOn) askForNotifications()
                },
                onFixOwls = fixOwls,
                onExport = { exportLauncher.launch("expensiarmus-${LocalDate.now()}.csv") },
                bottomBar = { tabBar(Tab.SETTINGS) },
            )
            route == SEARCH -> SearchScreen(
                expenses = data.expenses,
                onOpen = { push(VIEW + it) },
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
                    onOpen = { push(VIEW + it) },
                    onBack = ::pop,
                )
            }
            route.startsWith(VIEW) -> key(route) {
                ExpenseDetailScreen(
                    expense = data.find(route.removePrefix(VIEW)),
                    onEdit = { push(EDIT + it) },
                    onDelete = { vm.delete(it); pop() },
                    onBack = ::pop,
                )
            }
            route.startsWith(EDIT) -> key(route) {
                val existing = route.removePrefix(EDIT).ifEmpty { null }?.let(data::find)
                val handed = remember { prefill.also { prefill = null } }
                EditExpenseScreen(
                    existing = existing,
                    defaultDate = newExpenseDate,
                    history = data.expenses,
                    prefill = handed,
                    onSave = {
                        vm.upsert(it)
                        month = YearMonth.from(it.date)
                        pop()
                    },
                    onBack = ::pop,
                )
            }
            else -> {
                val view = if (route == CALENDAR) MonthView.CALENDAR else MonthView.OVERVIEW
                val summary = remember(data, month) { MonthlySummary.of(data, month) }
                val day = selectedDay?.takeIf { YearMonth.from(it) == month }
                val days = remember(data, month, day) {
                    DayDetails(
                        glance = ExpenseQueries.monthGlance(data.expenses, month, LocalDate.now()),
                        totals = ExpenseQueries.dailyTotals(data.expenses, month),
                        selected = day,
                        expenses = day?.let { ExpenseQueries.onDay(data.expenses, it) }.orEmpty(),
                    )
                }
                MonthScreen(
                    summary = summary,
                    view = view,
                    days = days,
                    onSelectDay = { selectedDay = it },
                    onPreviousMonth = { month = month.minusMonths(1) },
                    onNextMonth = { month = month.plusMonths(1) },
                    onAdd = { quickOpen = true },
                    onOpen = { push(VIEW + it) },
                    onCategory = { c, t -> push(CATEGORY + c.name + (t?.let { ":" + it.name } ?: "")) },
                    onSearch = { push(SEARCH) },
                    reminderOn = settings.reminderOn,
                    owlsBlocked = owlsBlocked,
                    onFixOwls = fixOwls,
                    snackbar = snackbar,
                    bottomBar = { tabBar(if (view == MonthView.CALENDAR) Tab.CALENDAR else Tab.LEDGER) },
                )
            }
        }
    }

    if (quickOpen) {
        QuickAddSheet(
            history = data.expenses,
            date = newExpenseDate,
            onSave = { e ->
                vm.upsert(e)
                month = YearMonth.from(e.date)
                quickOpen = false
                if (route != MONTH && route != CALENDAR) stack = MONTH
                scope.launch {
                    val result = snackbar.showSnackbar(
                        "Recorded ${e.title} · ${Money.format(e.myShareCents)}",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) vm.delete(e.id)
                }
            },
            onMore = {
                prefill = it
                quickOpen = false
                push(EDIT)
            },
            onDismiss = { quickOpen = false },
        )
    }
}
