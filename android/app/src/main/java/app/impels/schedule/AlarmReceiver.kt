package app.impels.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.impels.ImpelsApp
import app.impels.domain.ReminderStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires ONCE at the scheduled time and shows the full-screen ringing alarm.
 * One-shot: it does NOT reschedule itself. It only fires again if the user
 * taps Snooze (which schedules one more one-shot).
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1L)
        if (id < 0) return

        val pending = goAsync()
        val app = context.applicationContext as ImpelsApp
        val repo = app.container.repository

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val r = repo.getReminder(id) ?: return@launch
                if (r.status != ReminderStatus.ACTIVE) return@launch
                Notifications.showAlarm(context, r)
            } finally {
                pending.finish()
            }
        }
    }
}
