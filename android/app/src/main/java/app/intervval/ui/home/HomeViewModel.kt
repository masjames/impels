package app.intervval.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.intervval.data.ReminderRepository
import app.intervval.domain.Reminder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val focused: Reminder? = null,
    val queue: List<Reminder> = emptyList(),
    val done: List<Reminder> = emptyList()
)

class HomeViewModel(private val repo: ReminderRepository) : ViewModel() {

    val state: StateFlow<HomeUiState> =
        combine(repo.observeActive(), repo.observeDone()) { active, done ->
            HomeUiState(
                focused = active.firstOrNull { it.isFocused },
                queue = active.filterNot { it.isFocused },
                done = done
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun markDone(id: Long) = viewModelScope.launch { repo.markDone(id) }
    fun snooze(id: Long, minutes: Int) = viewModelScope.launch { repo.snooze(id, minutes) }
    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
    fun setFocus(id: Long) = viewModelScope.launch { repo.setFocus(id) }
    fun clearFocus() = viewModelScope.launch { repo.clearFocus() }
    fun restore(id: Long) = viewModelScope.launch { repo.restore(id) }

    class Factory(private val repo: ReminderRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(repo) as T
    }
}
