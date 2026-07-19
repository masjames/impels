package app.impels.data

import app.impels.domain.Reminder
import app.impels.domain.ReminderStatus
import app.impels.schedule.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for reminders. Persists to Room and keeps the alarm
 * schedule in sync. All scheduling side-effects live here so the UI/receivers
 * stay thin.
 */
class ReminderRepository(
    private val dao: ReminderDao,
    private val scheduler: ReminderScheduler
) {
    fun observeActive(): Flow<List<Reminder>> =
        dao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeDone(): Flow<List<Reminder>> =
        dao.observeDone().map { list -> list.map { it.toDomain() } }

    suspend fun getReminder(id: Long): Reminder? = dao.getById(id)?.toDomain()

    suspend fun add(title: String, fromWho: String?, intervalMinutes: Int, focus: Boolean = false): Long {
        val now = System.currentTimeMillis()
        val next = now + intervalMinutes * 60_000L
        if (focus) dao.clearFocus()
        val id = dao.insert(
            ReminderEntity(
                title = title.trim(),
                fromWho = fromWho?.trim()?.ifBlank { null },
                intervalMinutes = intervalMinutes,
                status = ReminderStatus.ACTIVE.name,
                isFocused = focus,
                createdAt = now,
                nextTriggerAt = next,
                snoozedUntil = null
            )
        )
        scheduler.schedule(id, next)
        return id
    }

    suspend fun update(reminder: Reminder) {
        val now = System.currentTimeMillis()
        val next = now + reminder.intervalMinutes * 60_000L
        val updated = reminder.copy(nextTriggerAt = next, snoozedUntil = null)
        dao.update(updated.toEntity())
        scheduler.cancel(reminder.id)
        if (updated.status == ReminderStatus.ACTIVE) scheduler.schedule(reminder.id, next)
    }

    suspend fun updateNextTrigger(id: Long, next: Long) = dao.updateNextTrigger(id, next)

    suspend fun markDone(id: Long) {
        val e = dao.getById(id) ?: return
        dao.update(e.copy(status = ReminderStatus.DONE.name, isFocused = false))
        scheduler.cancel(id)
    }

    suspend fun restore(id: Long) {
        val e = dao.getById(id) ?: return
        val now = System.currentTimeMillis()
        val next = now + e.intervalMinutes * 60_000L
        dao.update(e.copy(status = ReminderStatus.ACTIVE.name, nextTriggerAt = next, snoozedUntil = null))
        scheduler.schedule(id, next)
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        dao.delete(id)
    }

    suspend fun setFocus(id: Long) {
        dao.clearFocus()
        val e = dao.getById(id) ?: return
        dao.update(e.copy(isFocused = true))
    }

    suspend fun clearFocus() = dao.clearFocus()

    suspend fun snooze(id: Long, minutes: Int) {
        val e = dao.getById(id) ?: return
        val until = System.currentTimeMillis() + minutes * 60_000L
        dao.update(e.copy(snoozedUntil = until, nextTriggerAt = until))
        scheduler.cancel(id)
        scheduler.schedule(id, until)
    }

    /**
     * Boot / update: re-arm every active one-shot reminder. If its fire time
     * already passed while the device was off, fire it shortly after boot.
     */
    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        dao.getAllActive().forEach { e ->
            val next = if (e.nextTriggerAt <= now) now + 5_000L else e.nextTriggerAt
            if (next != e.nextTriggerAt) dao.updateNextTrigger(e.id, next)
            scheduler.schedule(e.id, next)
        }
    }
}
