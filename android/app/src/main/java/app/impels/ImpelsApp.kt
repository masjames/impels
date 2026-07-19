package app.impels

import android.app.Application
import app.impels.schedule.Notifications

class ImpelsApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.ensureChannel(this)
    }
}
