package app.impels.schedule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.impels.R
import app.impels.domain.Reminder

object Notifications {

    const val CHANNEL_ID = "alarms"
    val VIBRATION_PATTERN = longArrayOf(0, 500, 500)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Rings when a reminder is due."
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                setSound(alarmSound, attrs)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Shows the ringing alarm: a full-screen-intent notification that launches
     * AlarmActivity (which plays the looping sound + vibration). Done/Snooze
     * actions on the notification are the fallback when full-screen isn't shown.
     */
    fun showAlarm(context: Context, reminder: Reminder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notifId = reminder.id.toInt()

        val fullScreenIntent = PendingIntent.getActivity(
            context,
            notifId,
            AlarmActivity.intent(context, reminder.id, reminder.title, reminder.fromWho),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val donePi = actionPendingIntent(context, reminder.id, ActionReceiver.ACTION_DONE, notifId * 10 + 1)
        val snoozePi = actionPendingIntent(context, reminder.id, ActionReceiver.ACTION_SNOOZE, notifId * 10 + 2)

        val text = reminder.fromWho?.let { "From $it" } ?: "Reminder"

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_logo)
            .setContentTitle(reminder.title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVibrate(VIBRATION_PATTERN)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .addAction(0, "Dismiss", donePi)
            .addAction(0, "Snooze", snoozePi)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notif)
        } catch (_: SecurityException) {
            // Permission revoked between check and notify; ignore.
        }
    }

    fun cancel(context: Context, id: Long) {
        NotificationManagerCompat.from(context).cancel(id.toInt())
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
