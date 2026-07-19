package app.intervval.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Thin wrapper over AlarmManager. One pending exact alarm per reminder id.
 * Exact + allow-while-idle so nags survive Doze (reminder apps are permitted this).
 */
class ReminderScheduler(private val context: Context) {

    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(id: Long, triggerAt: Long) {
        val pi = pendingIntent(id)
        val canExact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            // No exact-alarm permission: fall back to inexact so nags still fire (a bit late).
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(id: Long) {
        am.cancel(pendingIntent(id))
    }

    private fun pendingIntent(id: Long): PendingIntent {
        val i = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_NAG
            putExtra(EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context,
            id.toInt(),
            i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_NAG = "app.intervval.NAG"
        const val EXTRA_ID = "id"
    }
}
