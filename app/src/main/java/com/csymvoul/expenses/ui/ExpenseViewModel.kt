package com.csymvoul.expenses.ui

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.csymvoul.expenses.data.AppSettings
import com.csymvoul.expenses.data.ExpenseStore
import com.csymvoul.expenses.domain.CsvExporter
import com.csymvoul.expenses.domain.CsvImporter
import com.csymvoul.expenses.domain.ImportResult
import com.csymvoul.expenses.domain.GoalAlerts
import com.csymvoul.expenses.model.AppData
import com.csymvoul.expenses.model.Category
import com.csymvoul.expenses.model.Expense
import com.csymvoul.expenses.reminder.GoalNotifier
import com.csymvoul.expenses.widget.LedgerWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.YearMonth
import java.util.UUID

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {
    private val store = ExpenseStore(File(app.filesDir, "expenses.json"))
    private val mutex = Mutex()
    private val _state = MutableStateFlow(AppData())
    val state: StateFlow<AppData> = _state.asStateFlow()

    private val loadJob = viewModelScope.launch {
        _state.value = withContext(Dispatchers.IO) { store.load() }
    }

    fun upsert(expense: Expense) = update { it.upsert(expense) }

    fun delete(id: String) = update { it.delete(id) }

    /** Sets a goal for [category] (null: the whole month) in [month] only, or from [month] onwards. */
    fun setGoal(category: Category?, month: YearMonth, cents: Long, onwards: Boolean) =
        update { it.copy(goals = it.goals.set(category, month, cents, onwards)) }

    /** Removes the goal for [category] in [month] only, or from [month] onwards. */
    fun removeGoal(category: Category?, month: YearMonth, onwards: Boolean) =
        update { it.copy(goals = it.goals.remove(category, month, onwards)) }

    private fun update(change: (AppData) -> AppData) {
        viewModelScope.launch {
            loadJob.join() // never overwrite the file before it has been read
            mutex.withLock {
                val changed = change(_state.value)
                // Warn once when a category of the current month reaches 80% of its goal, or goes over.
                val alerts = GoalAlerts.due(changed, YearMonth.now(), AppSettings.get(getApplication()).words)
                val next = GoalAlerts.markSent(changed, alerts)
                _state.value = next
                withContext(Dispatchers.IO) { store.save(next) }
                alerts.forEach { GoalNotifier.show(getApplication(), it) }
                refreshWidget()
            }
        }
    }

    /** Redraws the home-screen widget, if one is placed. */
    fun refreshWidget() = LedgerWidget.refresh(getApplication())

    /** Apparate, step 1: reads a CSV and works out what it would add. Null if the file can't be read. */
    suspend fun previewImport(resolver: ContentResolver, uri: Uri): ImportResult? = withContext(Dispatchers.IO) {
        runCatching {
            val text = resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: error("cannot open $uri")
            CsvImporter.import(text, _state.value.expenses) { UUID.randomUUID().toString() }
        }.getOrNull()
    }

    /** Apparate, step 2: adds the expenses the user confirmed. */
    fun addAll(expenses: List<Expense>) = update { it.copy(expenses = it.expenses + expenses) }

    /** Writes all expenses as CSV (UTF-8 with BOM so Excel shows Greek text correctly). */
    suspend fun exportCsv(resolver: ContentResolver, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = ("\uFEFF" + CsvExporter.toCsv(_state.value.expenses)).toByteArray(Charsets.UTF_8)
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("cannot open $uri")
        }.isSuccess
    }
}
