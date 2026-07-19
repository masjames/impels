package app.intervval.schedule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.intervval.MainActivity
import app.intervval.R
import app.intervval.domain.Reminder

object Notifications {

    const val CHANNEL_ID = "nags"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Repeat nags until you mark a task done."
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    fun showNag(context: Context, reminder: Reminder) {
        // Respect POST_NOTIFICATIONS (API 33+). If denied, silently skip.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notifId = reminder.id.toInt()

        val contentIntent = PendingIntent.getActivity(
            context,
            notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("openReminderId", reminder.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val donePi = actionPendingIntent(context, reminder.id, ActionReceiver.ACTION_DONE, notifId * 10 + 1)
        val snoozePi = actionPendingIntent(context, reminder.id, ActionReceiver.ACTION_SNOOZE, notifId * 10 + 2)

        val text = reminder.fromWho?.let { "From $it" } ?: "Tap to open"

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setContentTitle(reminder.title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOnlyAlertOnce(false)
            .setAutoCancel(false)
            .setContentIntent(contentIntent)
            .addAction(0, "Done", donePi)
            .addAction(0, "Snooze", snoozePi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notif)
        } catch (_: SecurityException) {
            // Permission revoked between check and notify; ignore.
        }
    }

    private fun actionPendingIntent(context: Context, id: Long, action: String, requestCode: Int): PendingIntent {
        val i = Intent(context, ActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderScheduler.EXTRA_ID, id)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
