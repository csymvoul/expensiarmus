package com.csymvoul.expenses.data

import android.content.Context
import com.csymvoul.expenses.domain.Money
import com.csymvoul.expenses.domain.MoneyStyle
import com.csymvoul.expenses.domain.Reminder
import com.csymvoul.expenses.domain.Words
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** What the user chose in Settings. */
data class Settings(
    val reminderOn: Boolean = true,
    val reminderTime: LocalTime = Reminder.TIME,
    val wizardWords: Boolean = true,
    val moneyStyle: MoneyStyle = MoneyStyle.ENGLISH,
    val theme: ThemeMode = ThemeMode.SYSTEM,
) {
    val words: Words get() = Words.of(wizardWords)

    companion object {
        /** Builds settings from stored values, falling back to the defaults for anything missing or invalid. */
        fun fromStored(reminderOn: Boolean?, time: String?, wizard: Boolean?, money: String?, theme: String?) = Settings(
            reminderOn = reminderOn ?: true,
            reminderTime = time?.let(Reminder::parseTime) ?: Reminder.TIME,
            wizardWords = wizard ?: true,
            moneyStyle = MoneyStyle.entries.firstOrNull { it.name == money } ?: MoneyStyle.ENGLISH,
            theme = ThemeMode.entries.firstOrNull { it.name == theme } ?: ThemeMode.SYSTEM,
        )
    }
}

/** Settings kept in shared preferences, observable by the UI. */
object AppSettings {
    private const val PREFS = "settings"
    private const val REMINDER = "daily_reminder" // same key as before settings existed
    private const val TIME = "reminder_time"
    private const val WIZARD = "wizard_words"
    private const val MONEY = "money_style"
    private const val THEME = "theme"

    private val _state = MutableStateFlow(Settings())
    val state: StateFlow<Settings> = _state.asStateFlow()

    @Volatile private var loaded = false

    fun get(context: Context): Settings {
        if (!loaded) load(context)
        return _state.value
    }

    fun update(context: Context, change: (Settings) -> Settings) {
        val next = change(get(context))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(REMINDER, next.reminderOn)
            .putString(TIME, "%02d:%02d".format(next.reminderTime.hour, next.reminderTime.minute))
            .putBoolean(WIZARD, next.wizardWords)
            .putString(MONEY, next.moneyStyle.name)
            .putString(THEME, next.theme.name)
            .apply()
        publish(next)
    }

    private fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        publish(
            Settings.fromStored(
                reminderOn = if (p.contains(REMINDER)) p.getBoolean(REMINDER, true) else null,
                time = p.getString(TIME, null),
                wizard = if (p.contains(WIZARD)) p.getBoolean(WIZARD, true) else null,
                money = p.getString(MONEY, null),
                theme = p.getString(THEME, null),
            ),
        )
    }

    private fun publish(settings: Settings) {
        Money.style = settings.moneyStyle
        _state.value = settings
        loaded = true
    }
}
