package app.intervval.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.intervval.IntervvalApp
import app.intervval.domain.ReminderStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires on each nag. Posts the notification, then schedules the NEXT nag.
 * This self-rescheduling is what makes intervval nag repeatedly until Done.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1L)
        if (id < 0) return

        val pending = goAsync()
        val app = context.applicationContext as IntervvalApp
        val repo = app.container.repository
        val scheduler = app.container.scheduler

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val r = repo.getReminder(id) ?: return@launch
                if (r.status != ReminderStatus.ACTIVE) return@launch

                val now = System.currentTimeMillis()

                // Still snoozed? re-arm at snooze end, don't nag yet.
                val snoozedUntil = r.snoozedUntil
                if (snoozedUntil != null && snoozedUntil > now) {
                    scheduler.schedule(id, snoozedUntil)
                    return@launch
                }

                Notifications.showNag(context, r)

                val next = now + r.intervalMinutes * 60_000L
                repo.updateNextTrigger(id, next)
                scheduler.schedule(id, next)
            } finally {
                pending.finish()
            }
        }
    }
}
