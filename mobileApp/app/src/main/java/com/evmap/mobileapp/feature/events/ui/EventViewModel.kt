package com.evmap.mobileapp.feature.events.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.EventDto
import com.evmap.mobileapp.core.data.dto.EventPhotoDto
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.events.data.EventsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventUiState(
    val loading: Boolean = false,
    val event: EventDto? = null,
    val photos: List<EventPhotoDto> = emptyList(),
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
            .onSuccess { dto ->
                val photos = repo.getPhotos(id).getOrDefault(emptyList())
                _state.value = EventUiState(loading = false, event = dto, photos = photos)
            }
            .onFailure { e -> _state.value = EventUiState(loading = false, error = e.toUserMessage()) }
    }

    fun share(id: Long) = viewModelScope.launch {
        repo.share(id)
            .onSuccess {
                _state.update { state ->
                    state.copy(event = state.event?.copy(repostsCount = state.event.repostsCount + 1))
                }
            }
            .onFailure { error ->
                _state.update { it.copy(error = error.toUserMessage()) }
            }
    }
}
