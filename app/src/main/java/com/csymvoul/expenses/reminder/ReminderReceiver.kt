package com.csymvoul.expenses.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Shows the evening owl, and re-arms the alarm after it fires, a reboot, a clock change or a permission change. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ReminderScheduler.ACTION_REMIND && ReminderScheduler.isEnabled(context)) {
            OwlNotifier.show(context)
        }
        ReminderScheduler.schedule(context)
    }
}
