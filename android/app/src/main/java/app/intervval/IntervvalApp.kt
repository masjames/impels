package app.intervval

import android.app.Application
import app.intervval.schedule.Notifications

class IntervvalApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.ensureChannel(this)
    }
}
