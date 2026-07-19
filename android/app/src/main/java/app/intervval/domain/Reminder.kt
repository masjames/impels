package app.intervval.domain

enum class ReminderStatus { ACTIVE, DONE }

/** Interval in minutes between repeat nags. */
enum class IntervalOption(val minutes: Int, val label: String) {
    M5(5, "Every 5 min"),
    M10(10, "Every 10 min"),
    M15(15, "Every 15 min"),
    M30(30, "Every 30 min"),
    M60(60, "Every 1 hour");

    companion object {
        fun fromMinutes(m: Int): IntervalOption = entries.firstOrNull { it.minutes == m } ?: M15
    }
}

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
