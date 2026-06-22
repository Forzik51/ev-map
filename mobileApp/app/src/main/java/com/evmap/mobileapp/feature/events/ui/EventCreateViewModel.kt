package com.evmap.mobileapp.feature.events.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.CreateEventRequest
import com.evmap.mobileapp.core.data.dto.EventCategoryDto
import com.evmap.mobileapp.core.data.dto.SearchLocationDto
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.events.data.EventsRepository
import com.evmap.mobileapp.feature.events.data.LocationSearchRepository
import core.ui.model.EventDraftUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class EventCreateUiState(
    val draft: EventDraftUi = EventDraftUi(
        title = "",
        subtitle = "",
        description = "",
        date = "",
        location = "",
    ),
    val categories: List<EventCategoryDto> = emptyList(),
    val locationResults: List<SearchLocationDto> = emptyList(),
    val locationQuery: String = "",
    val isLocationPickerOpen: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val createdEventId: Long? = null,
)

@HiltViewModel
class EventCreateViewModel @Inject constructor(
    private val repository: EventsRepository,
    private val locationSearchRepository: LocationSearchRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(EventCreateUiState())
    val state: StateFlow<EventCreateUiState> = _state

    init {
        loadCategories()
    }

    fun updateTitle(value: String) = updateDraft { it.copy(title = value) }

    fun updateSubtitle(value: String) = updateDraft { it.copy(subtitle = value) }

    fun updateDescription(value: String) = updateDraft { it.copy(description = value) }

    fun updateDate(value: String) = updateDraft { it.copy(date = value) }

    fun updateCategoriesQuery(value: String) = updateDraft { it.copy(categoriesQuery = value) }

    fun openLocationPicker() {
        _state.update { it.copy(isLocationPickerOpen = true, locationQuery = "", locationResults = emptyList()) }
    }

    fun closeLocationPicker() {
        _state.update { it.copy(isLocationPickerOpen = false) }
    }

    fun updateLocationQuery(value: String) {
        _state.update { it.copy(locationQuery = value) }
        if (value.length < MIN_LOCATION_QUERY_LENGTH) {
            _state.update { it.copy(locationResults = emptyList()) }
            return
        }

        viewModelScope.launch {
            locationSearchRepository.searchLocations(value)
                .onSuccess { locations ->
                    _state.update { it.copy(locationResults = locations, error = null) }
                }
                .onFailure { error ->
                    _state.update { it.copy(error = error.toUserMessage()) }
                }
        }
    }

    fun selectLocation(location: SearchLocationDto) {
        updateDraft { it.copy(location = location.name, locationId = location.id) }
        _state.update { it.copy(isLocationPickerOpen = false, locationQuery = "", locationResults = emptyList()) }
    }

    fun toggleCategory(category: String) = updateDraft { draft ->
        val categories = if (category in draft.categories) {
            draft.categories - category
        } else {
            draft.categories + category
        }
        draft.copy(categories = categories)
    }

    fun removePhoto(index: Int) = updateDraft { draft ->
        draft.copy(media = draft.media.toMutableList().also { if (index in it.indices) it.removeAt(index) })
    }

    fun createEvent() {
        val current = _state.value
        val request = current.toRequest()
        if (request == null) return

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repository.createEvent(request)
                .onSuccess { eventId ->
                    _state.update { it.copy(loading = false, createdEventId = eventId) }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = error.toUserMessage()) }
                }
        }
    }

    fun consumeCreatedEvent() {
        _state.update { it.copy(createdEventId = null) }
    }

    private fun loadCategories() = viewModelScope.launch {
        repository.getCategories()
            .onSuccess { categories ->
                _state.update { state ->
                    state.copy(
                        categories = categories,
                        draft = state.draft.copy(availableCategories = categories.map { it.name })
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(error = error.toUserMessage()) }
            }
    }

    private fun updateDraft(transform: (EventDraftUi) -> EventDraftUi) {
        _state.update { it.copy(draft = transform(it.draft), error = null) }
    }

    private fun EventCreateUiState.toRequest(): CreateEventRequest? {
        val selectedCategoryIds = categories
            .filter { it.name in draft.categories }
            .map { it.id }
        val locationId = draft.locationId

        val validationError = when {
            draft.title.isBlank() -> "Enter event title"
            draft.description.isBlank() -> "Enter event description"
            draft.date.isBlank() -> "Pick event date"
            locationId == null -> "Select event location"
            selectedCategoryIds.isEmpty() -> "Select at least one category"
            else -> null
        }

        if (validationError != null) {
            _state.update { it.copy(error = validationError) }
            return null
        }

        return CreateEventRequest(
            name = draft.title.trim(),
            description = draft.description.trim(),
            startsAt = draft.date.toEventInstant(),
            locationId = requireNotNull(locationId),
            categoryIds = selectedCategoryIds,
        )
    }

    private fun String.toEventInstant(): String {
        val localDate = runCatching { LocalDate.parse(this) }
            .getOrElse {
                LocalDate.parse(this, DateTimeFormatter.ofPattern("MM/dd/yyyy"))
            }
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
    }

    private companion object {
        const val MIN_LOCATION_QUERY_LENGTH = 2
    }
}
