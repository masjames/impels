package app.impels.domain

enum class ReminderStatus { ACTIVE, DONE }

/** One-shot delay in minutes: "remind me in X". Not recurring. */
enum class IntervalOption(val minutes: Int, val label: String) {
    M5(5, "In 5 min"),
    M10(10, "In 10 min"),
    M15(15, "In 15 min"),
    M30(30, "In 30 min"),
    M60(60, "In 1 hour");

    companion object {
        fun fromMinutes(m: Int): IntervalOption = entries.firstOrNull { it.minutes == m } ?: M15
    }
}

/** Snooze quick-actions offered on the ringing alarm screen. */
val SNOOZE_OPTIONS = listOf(5, 10, 15)

data class Reminder(
    val id: Long = 0,
    val title: String,
    val fromWho: String? = null,
    val intervalMinutes: Int = 15,
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val isFocused: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val nextTriggerAt: Long = 0L,
    val snoozedUntil: Long? = null
)
