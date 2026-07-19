package app.impels.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
abstract class ImpelsDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: ImpelsDatabase? = null

        fun get(context: Context): ImpelsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ImpelsDatabase::class.java,
                    "impels.db"
                ).build().also { INSTANCE = it }
            }
    }
}
