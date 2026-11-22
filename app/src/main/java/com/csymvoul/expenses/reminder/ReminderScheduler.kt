package com.csymvoul.expenses.reminder

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.csymvoul.expenses.domain.Reminder
import java.time.ZonedDateTime

/**
 * Arms one exact alarm for the next 21:30 (inexact if Android does not allow exact ones);
 * [ReminderReceiver] re-arms it each time it fires, after a reboot and after clock changes.
 */
object ReminderScheduler {
    const val CHANNEL = "daily_reminder"
    const val ACTION_REMIND = "com.csymvoul.expenses.action.REMIND"
    private const val PREFS = "settings"
    private const val KEY_ENABLED = "daily_reminder"
    private const val KEY_ASKED = "notification_permission_asked"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(context: Context) = prefs(context).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) schedule(context) else cancel(context)
    }

    /** True only the first time it is called, so the permission prompt is shown once on its own. */
    fun firstPermissionAsk(context: Context): Boolean {
        if (prefs(context).getBoolean(KEY_ASKED, false)) return false
        prefs(context).edit().putBoolean(KEY_ASKED, true).apply()
        return true
    }

    fun schedule(context: Context) {
        if (!isEnabled(context)) return
        val at = Reminder.nextTrigger(ZonedDateTime.now()).toInstant().toEpochMilli()
        val alarms = context.getSystemService(AlarmManager::class.java)
        val exact = Reminder.useExactAlarm(
            Build.VERSION.SDK_INT,
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarms.canScheduleExactAlarms(),
        )
        if (exact) {
            setExact(alarms, at, alarmIntent(context))
        } else {
            // Fallback when exact alarms are not allowed: Android may deliver this one late.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, alarmIntent(context))
        }
    }

    // Only called after Reminder.useExactAlarm confirmed exact alarms are allowed.
    @SuppressLint("ScheduleExactAlarm", "MissingPermission")
    private fun setExact(alarms: AlarmManager, at: Long, intent: PendingIntent) {
        alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context))
    }

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Owl reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Evening reminder to record the day's expenses"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
