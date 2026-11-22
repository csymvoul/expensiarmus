package com.csymvoul.expenses.reminder

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.csymvoul.expenses.MainActivity
import com.csymvoul.expenses.R
import com.csymvoul.expenses.data.ExpenseStore
import com.csymvoul.expenses.domain.Reminder
import java.io.File
import java.time.LocalDate

/** Builds and shows the owl notification with today's text. */
object OwlNotifier {
    /** False when notifications are switched off for the app (or not yet allowed on Android 13+). */
    fun canNotify(context: Context): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Shows the owl now. Returns false, showing nothing, when notifications are blocked. */
    fun show(context: Context): Boolean {
        if (!canNotify(context)) return false
        ReminderScheduler.ensureChannel(context)
        val data = ExpenseStore(File(context.filesDir, "expenses.json")).load()
        val message = Reminder.message(data.expenses, LocalDate.now())
        val open = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_RECORD, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, ReminderScheduler.CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_quill)
            .setColor(0xFF7B1E1E.toInt())
            .setContentTitle(message.title)
            .setContentText(message.text)
            .setStyle(Notification.BigTextStyle().bigText(message.text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(1, notification)
        return true
    }
}
