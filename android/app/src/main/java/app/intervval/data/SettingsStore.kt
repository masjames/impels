package app.intervval.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** App settings: default nag interval and default snooze length. */
class SettingsStore(private val context: Context) {

    private val defaultInterval = intPreferencesKey("default_interval_minutes")
    private val defaultSnooze = intPreferencesKey("default_snooze_minutes")

    val defaultIntervalMinutes: Flow<Int> =
        context.dataStore.data.map { it[defaultInterval] ?: 15 }

    val defaultSnoozeMinutes: Flow<Int> =
        context.dataStore.data.map { it[defaultSnooze] ?: 10 }

    suspend fun setDefaultInterval(minutes: Int) {
        context.dataStore.edit { it[defaultInterval] = minutes }
    }

    suspend fun setDefaultSnooze(minutes: Int) {
        context.dataStore.edit { it[defaultSnooze] = minutes }
    }

    /** One-shot read for use inside receivers. */
    suspend fun snoozeMinutesOnce(): Int = defaultSnoozeMinutes.first()
}
