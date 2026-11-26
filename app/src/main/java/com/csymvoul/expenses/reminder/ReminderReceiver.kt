package com.csymvoul.expenses.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.csymvoul.expenses.update.Updater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Shows the evening owl, and re-arms the alarm after it fires, a reboot, a clock change or a permission change.
 * It also runs the daily update check, when one is due.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ReminderScheduler.ACTION_REMIND && ReminderScheduler.isEnabled(context)) {
            OwlNotifier.show(context)
            // On the 1st a second owl sums up the month just finished.
            if (java.time.LocalDate.now().dayOfMonth == 1) OwlNotifier.showRecap(context)
        }
        ReminderScheduler.schedule(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Updater.checkIfDue(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}
