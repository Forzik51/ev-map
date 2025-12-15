package com.evmap.mobileapp.feature.events.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.EventDto
import com.evmap.mobileapp.feature.events.data.EventsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventUiState(
    val loading: Boolean = false,
    val event: EventDto? = null,
    val error: String? = null
)

@HiltViewModel
class EventViewModel @Inject constructor(
    private val repo: EventsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(EventUiState());
    val state: StateFlow<EventUiState> = _state

    fun load(id: Long) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)

        repo.getEvent(id)
            .onSuccess { dto -> _state.value = EventUiState(loading = false, event = dto) }
            .onFailure { e -> _state.value = EventUiState(loading = false, error = e.message ?: "Unknown error") }
    }
}