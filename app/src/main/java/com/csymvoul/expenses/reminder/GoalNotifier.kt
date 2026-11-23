package com.csymvoul.expenses.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.csymvoul.expenses.MainActivity
import com.csymvoul.expenses.R
import com.csymvoul.expenses.domain.GoalAlert

/** Shows goal warnings (80% and over the goal) as notifications, or as a toast if those are blocked. */
object GoalNotifier {
    private const val CHANNEL = "goal_warnings"

    fun show(context: Context, alert: GoalAlert) {
        if (!OwlNotifier.canNotify(context)) {
            Toast.makeText(context, "${alert.title}\n${alert.body}", Toast.LENGTH_LONG).show()
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Goal warnings", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "When a category reaches 80% of its monthly goal, and when it goes over"
            },
        )
        val open = PendingIntent.getActivity(
            context,
            2,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_quill)
            .setColor(0xFF7B1E1E.toInt())
            .setContentTitle(alert.title)
            .setContentText(alert.body)
            .setStyle(Notification.BigTextStyle().bigText(alert.body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        // One notification per category and month; the 100% warning replaces the 80% one.
        manager.notify("goal|${alert.category.name}|${alert.month}".hashCode(), notification)
    }
}
