package app.impels.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.impels.domain.Reminder
import app.impels.domain.ReminderStatus

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val fromWho: String?,
    val intervalMinutes: Int,
    val status: String,
    val isFocused: Boolean,
    val createdAt: Long,
    val nextTriggerAt: Long,
    val snoozedUntil: Long?
)

fun ReminderEntity.toDomain() = Reminder(
    id = id,
    title = title,
    fromWho = fromWho,
    intervalMinutes = intervalMinutes,
    status = ReminderStatus.valueOf(status),
    isFocused = isFocused,
    createdAt = createdAt,
    nextTriggerAt = nextTriggerAt,
    snoozedUntil = snoozedUntil
)

fun Reminder.toEntity() = ReminderEntity(
    id = id,
    title = title,
    fromWho = fromWho,
    intervalMinutes = intervalMinutes,
    status = status.name,
    isFocused = isFocused,
    createdAt = createdAt,
    nextTriggerAt = nextTriggerAt,
    snoozedUntil = snoozedUntil
)
