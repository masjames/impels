package app.impels.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.impels.data.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(val intervalMinutes: Int = 15, val snoozeMinutes: Int = 10)

class SettingsViewModel(private val settings: SettingsStore) : ViewModel() {

    val state: StateFlow<SettingsUiState> =
        combine(settings.defaultIntervalMinutes, settings.defaultSnoozeMinutes) { i, s ->
            SettingsUiState(i, s)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setInterval(m: Int) = viewModelScope.launch { settings.setDefaultInterval(m) }
    fun setSnooze(m: Int) = viewModelScope.launch { settings.setDefaultSnooze(m) }

    class Factory(private val settings: SettingsStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(settings) as T
    }
}
