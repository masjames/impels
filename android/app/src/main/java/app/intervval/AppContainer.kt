package app.intervval

import android.content.Context
import app.intervval.data.IntervvalDatabase
import app.intervval.data.ReminderRepository
import app.intervval.data.SettingsStore
import app.intervval.schedule.ReminderScheduler

/** Manual dependency container. No DI framework by design. */
class AppContainer(context: Context) {
    private val db = IntervvalDatabase.get(context)
    val scheduler = ReminderScheduler(context)
    val settings = SettingsStore(context)
    val repository = ReminderRepository(db.reminderDao(), scheduler)
}
