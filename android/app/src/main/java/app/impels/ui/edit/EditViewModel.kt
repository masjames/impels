package app.impels.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.impels.data.ReminderRepository
import app.impels.data.SettingsStore
import app.impels.domain.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class EditUiState(
    val id: Long = 0,
    val title: String = "",
    val fromWho: String = "",
    val intervalMinutes: Int = 15,
    val isExisting: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null
)

class EditViewModel(
    private val repo: ReminderRepository,
    private val settings: SettingsStore
) : ViewModel() {

    private val _state = MutableStateFlow(EditUiState())
    val state: StateFlow<EditUiState> = _state

    fun load(id: Long?) {
        viewModelScope.launch {
            if (id == null || id <= 0) {
                _state.value = EditUiState(intervalMinutes = settings.defaultIntervalMinutes.first())
            } else {
                val r = repo.getReminder(id) ?: return@launch
                _state.value = EditUiState(
                    id = r.id, title = r.title, fromWho = r.fromWho ?: "",
                    intervalMinutes = r.intervalMinutes, isExisting = true
                )
            }
        }
    }

    fun onTitle(v: String) { _state.value = _state.value.copy(title = v, error = null) }
    fun onFromWho(v: String) { _state.value = _state.value.copy(fromWho = v) }
    fun onInterval(m: Int) { _state.value = _state.value.copy(intervalMinutes = m) }

    fun save(focus: Boolean, onDone: () -> Unit) {
        val s = _state.value
        if (s.title.isBlank()) { _state.value = s.copy(error = "Add a title"); return }
        if (s.saving) return
        _state.value = s.copy(saving = true)
        viewModelScope.launch {
            if (s.isExisting) {
                repo.update(
                    Reminder(id = s.id, title = s.title, fromWho = s.fromWho.ifBlank { null }, intervalMinutes = s.intervalMinutes)
                )
            } else {
                repo.add(s.title, s.fromWho.ifBlank { null }, s.intervalMinutes, focus)
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = _state.value.id
        if (id <= 0) { onDone(); return }
        viewModelScope.launch { repo.delete(id); onDone() }
    }

    class Factory(
        private val repo: ReminderRepository,
        private val settings: SettingsStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = EditViewModel(repo, settings) as T
    }
}
