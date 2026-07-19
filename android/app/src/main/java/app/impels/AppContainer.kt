package app.impels

import android.content.Context
import app.impels.data.ImpelsDatabase
import app.impels.data.ReminderRepository
import app.impels.data.SettingsStore
import app.impels.schedule.ReminderScheduler

/** Manual dependency container. No DI framework by design. */
class AppContainer(context: Context) {
    private val db = ImpelsDatabase.get(context)
    val scheduler = ReminderScheduler(context)
    val settings = SettingsStore(context)
    val repository = ReminderRepository(db.reminderDao(), scheduler)
}
