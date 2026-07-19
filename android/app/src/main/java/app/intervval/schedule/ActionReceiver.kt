package app.intervval.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import app.intervval.IntervvalApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles the Done / Snooze actions tapped on a nag notification. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1L)
        if (id < 0) return
        val action = intent.action ?: return

        val pending = goAsync()
        val app = context.applicationContext as IntervvalApp
        val repo = app.container.repository
        val settings = app.container.settings

        // Cancel the visible notification immediately.
        NotificationManagerCompat.from(context).cancel(id.toInt())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_DONE -> repo.markDone(id)
                    ACTION_SNOOZE -> repo.snooze(id, settings.snoozeMinutesOnce())
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "app.intervval.action.DONE"
        const val ACTION_SNOOZE = "app.intervval.action.SNOOZE"
    }
}
