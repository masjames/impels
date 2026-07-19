package app.intervval.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ReminderEntity::class], version = 1, exportSchema = false)
abstract class IntervvalDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: IntervvalDatabase? = null

        fun get(context: Context): IntervvalDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    IntervvalDatabase::class.java,
                    "intervval.db"
                ).build().also { INSTANCE = it }
            }
    }
}
