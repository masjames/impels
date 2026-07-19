package app.impels.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE status='ACTIVE' ORDER BY isFocused DESC, createdAt DESC")
    fun observeActive(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE status='DONE' ORDER BY createdAt DESC")
    fun observeDone(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id=:id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE status='ACTIVE'")
    suspend fun getAllActive(): List<ReminderEntity>

    @Insert
    suspend fun insert(e: ReminderEntity): Long

    @Update
    suspend fun update(e: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id=:id")
    suspend fun delete(id: Long)

    @Query("UPDATE reminders SET isFocused=0 WHERE isFocused=1")
    suspend fun clearFocus()

    @Query("UPDATE reminders SET nextTriggerAt=:next WHERE id=:id")
    suspend fun updateNextTrigger(id: Long, next: Long)
}
