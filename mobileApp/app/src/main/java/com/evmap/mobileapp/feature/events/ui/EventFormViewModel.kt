package com.evmap.mobileapp.feature.events.ui

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.CreateEventRequest
import com.evmap.mobileapp.core.data.dto.EventCategoryDto
import com.evmap.mobileapp.core.data.dto.EventPhotoDto
import com.evmap.mobileapp.core.data.dto.SearchLocationDto
import com.evmap.mobileapp.core.data.media.ImageMultipart
import com.evmap.mobileapp.feature.events.data.EventsRepository
import com.evmap.mobileapp.feature.events.data.LocationSearchRepository
import com.evmap.mobileapp.feature.events.data.eventError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class PendingEventPhoto(
    val uri: Uri,
    val uploadId: String = UUID.randomUUID().toString(),
    val error: String? = null,
)

data class EventFormState(
    val editing: Boolean,
    val loading: Boolean = true,
    val ready: Boolean = false,
    val saving: Boolean = false,
    val title: String = "",
    val description: String = "",
    val startsAt: Instant? = null,
    val endsAt: Instant? = null,
    val locationId: Long? = null,
    val locationName: String = "",
    val categories: List<EventCategoryDto> = emptyList(),
    val categoryIds: Set<Long> = emptySet(),
    val existingPhotos: List<EventPhotoDto> = emptyList(),
    val pendingPhotos: List<PendingEventPhoto> = emptyList(),
    val photosToDelete: Set<Long> = emptySet(),
    val locationQuery: String = "",
    val locationResults: List<SearchLocationDto> = emptyList(),
    val locationSearching: Boolean = false,
    val locationError: String? = null,
    val error: String? = null,
    val savedEventId: Long? = null,
    val finishedEventId: Long? = null,
    val creationUnconfirmed: Boolean = false,
)

@HiltViewModel
class EventFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val events: EventsRepository,
    private val locations: LocationSearchRepository,
    private val photoUploader: ImageMultipart,
) : ViewModel() {
    private val editingId = savedStateHandle.get<Long>("eventId")
    private val _state = MutableStateFlow(EventFormState(editing = editingId != null))
    val state = _state.asStateFlow()
    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        if (loadJob?.isActive == true) return
        _state.update { it.copy(loading = true, error = null) }
        loadJob = viewModelScope.launch {
            try {
                val event = editingId?.let { events.getEvent(it).getOrThrow() }
                val categories = events.getCategories().getOrThrow()
                val photos = editingId?.let { events.getPhotos(it).getOrThrow() }.orEmpty()
                _state.update {
                    it.copy(
                        loading = false,
                        ready = true,
                        title = event?.name.orEmpty(),
                        description = event?.description.orEmpty(),
                        startsAt = event?.startsAt?.let(Instant::parse),
                        endsAt = event?.endsAt?.let(Instant::parse),
                        locationId = event?.locationId,
                        locationName = event?.locationName.orEmpty(),
                        categories = (categories + event?.categories.orEmpty()).distinctBy { category -> category.id },
                        categoryIds = event?.categories.orEmpty().map { category -> category.id }.toSet(),
                        existingPhotos = photos,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, ready = false, error = e.eventError()) }
            }
        }
    }

    fun updateTitle(title: String) = updateFields { it.copy(title = title) }
    fun updateDescription(description: String) = updateFields { it.copy(description = description) }
    fun updateStartDate(date: LocalDate) = updateFields { it.copy(startsAt = onEventDate(it.startsAt, date)) }
    fun updateEndDate(date: LocalDate) = updateFields { it.copy(endsAt = onEventDate(it.endsAt ?: it.startsAt, date)) }
    fun clearEndDate() = updateFields { it.copy(endsAt = null) }

    fun toggleCategory(id: Long) = updateFields {
        val ids = if (id in it.categoryIds) it.categoryIds - id else it.categoryIds + id
        it.copy(categoryIds = ids)
    }

    fun selectLocation(location: SearchLocationDto) = updateFields {
        it.copy(locationId = location.id, locationName = location.name)
    }

    fun searchLocation(query: String) {
        searchJob?.cancel()
        val term = query.trim()
        _state.update {
            it.copy(locationQuery = query, locationResults = emptyList(), locationSearching = term.length >= 2, locationError = null)
        }
        if (term.length < 2) return
        searchJob = viewModelScope.launch {
            delay(250)
            val result = locations.searchLocations(term)
            currentCoroutineContext().ensureActive()
            result.onSuccess { results ->
                _state.update { it.copy(locationResults = results, locationSearching = false) }
            }.onFailure { error ->
                _state.update { it.copy(locationError = error.eventError(), locationSearching = false) }
            }
        }
    }

    fun addPhotos(uris: List<Uri>) = updatePhotos { state ->
        val pendingUris = state.pendingPhotos.map { it.uri }.toSet()
        val added = uris.distinct().filterNot { it in pendingUris }.map { PendingEventPhoto(it) }
        state.copy(pendingPhotos = state.pendingPhotos + added)
    }

    fun removePendingPhoto(uploadId: String) = updatePhotos { state ->
        state.copy(pendingPhotos = state.pendingPhotos.filterNot { it.uploadId == uploadId })
    }

    fun removeExistingPhoto(photoId: Long) = updatePhotos { state ->
        state.copy(photosToDelete = state.photosToDelete + photoId)
    }

    fun undoPhotoRemovals() = updatePhotos { it.copy(photosToDelete = emptySet()) }

    fun save() {
        val state = _state.value
        if (!state.ready || state.saving || state.creationUnconfirmed) return
        val request = if (state.savedEventId == null) {
            try {
                state.toRequest()
            } catch (e: IllegalArgumentException) {
                _state.update { it.copy(error = e.message) }
                return
            }
        } else null

        _state.update { state ->
            state.copy(saving = true, error = null, pendingPhotos = state.pendingPhotos.map { it.copy(error = null) })
        }
        viewModelScope.launch {
            try {
                val eventId = state.savedEventId ?: saveFields(checkNotNull(request)) ?: return@launch
                _state.update { it.copy(savedEventId = eventId) }
                val deletionError = savePhotos(eventId)
                _state.update {
                    val photosSaved = it.pendingPhotos.isEmpty() && it.photosToDelete.isEmpty()
                    it.copy(saving = false, error = deletionError, finishedEventId = eventId.takeIf { photosSaved })
                }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    private suspend fun saveFields(request: CreateEventRequest): Long? {
        val id = editingId
        val result = if (id == null) events.createEvent(request) else events.updateEvent(id, request).map { id }
        return result.getOrElse { error ->
            val creationUnconfirmed = id == null && (error !is HttpException || error.code() >= 500 || error.code() == 408)
            _state.update {
                it.copy(
                    creationUnconfirmed = creationUnconfirmed,
                    error = if (creationUnconfirmed) "Check All events to see if your event was created before trying again."
                    else error.eventError(),
                )
            }
            null
        }
    }

    private suspend fun savePhotos(eventId: Long): String? {
        val state = _state.value
        var deletionError: String? = null
        for (photoId in state.photosToDelete) {
            events.deletePhoto(eventId, photoId).onSuccess {
                _state.update {
                    it.copy(
                        existingPhotos = it.existingPhotos.filterNot { photo -> photo.id == photoId },
                        photosToDelete = it.photosToDelete - photoId,
                    )
                }
            }.onFailure { deletionError = it.eventError() }
        }
        for (photo in state.pendingPhotos) {
            try {
                val file = photoUploader.filePart(photo.uri)
                val uploaded = events.uploadPhoto(eventId, photo.uploadId, file).getOrThrow()
                _state.update {
                    it.copy(
                        existingPhotos = (it.existingPhotos + uploaded).distinctBy { item -> item.id },
                        pendingPhotos = it.pendingPhotos.filterNot { item -> item.uploadId == photo.uploadId },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val error = e.eventError()
                _state.update { state ->
                    state.copy(pendingPhotos = state.pendingPhotos.map {
                        if (it.uploadId == photo.uploadId) it.copy(error = error) else it
                    })
                }
            }
        }
        return deletionError
    }

    fun finishWithSavedPhotos() {
        val state = _state.value
        if (!state.saving && state.savedEventId != null) {
            _state.update { it.copy(finishedEventId = state.savedEventId) }
        }
    }

    private fun updateFields(update: (EventFormState) -> EventFormState) {
        _state.update { if (it.saving || it.savedEventId != null) it else update(it).copy(error = null) }
    }

    private fun updatePhotos(update: (EventFormState) -> EventFormState) {
        _state.update { if (it.saving) it else update(it) }
    }
}

internal fun EventFormState.toRequest(): CreateEventRequest {
    val name = title.trim()
    val text = description.trim()
    require(name.isNotEmpty() && name.length <= 30) { "Enter a title up to 30 characters." }
    require(text.isNotEmpty() && text.length <= 500) { "Enter a description up to 500 characters." }
    val start = requireNotNull(startsAt) { "Select the start date." }
    require(endsAt == null || !endsAt.isBefore(start)) { "The end date must be on or after the start." }
    val location = requireNotNull(locationId) { "Select a location." }
    require(categoryIds.isNotEmpty()) { "Select at least one category." }
    return CreateEventRequest(name, text, start.toString(), endsAt?.toString(), location, categoryIds.toList())
}

private fun onEventDate(previous: Instant?, date: LocalDate): Instant {
    val zone = ZoneId.systemDefault()
    val previousDateTime = previous?.atZone(zone)
    if (previousDateTime != null && previousDateTime.toLocalDate() == date) return previousDateTime.toInstant()
    return if (previousDateTime == null) date.atStartOfDay(zone).toInstant()
    else previousDateTime.with(date).toInstant()
}
