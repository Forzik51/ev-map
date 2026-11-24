package com.evmap.mobileapp.feature.events.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.EventCommentDto
import com.evmap.mobileapp.core.data.dto.EventDto
import com.evmap.mobileapp.core.data.dto.EventPhotoDto
import com.evmap.mobileapp.core.data.session.TokenStore
import com.evmap.mobileapp.feature.events.data.EventsRepository
import com.evmap.mobileapp.feature.events.data.eventError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class EventUiState(
    val loading: Boolean = true,
    val event: EventDto? = null,
    val canManage: Boolean = false,
    val loadError: String? = null,
    val photos: List<EventPhotoDto> = emptyList(),
    val photosError: String? = null,
    val comments: List<EventCommentDto> = emptyList(),
    val nextCommentPage: Int? = 0,
    val loadingComments: Boolean = false,
    val commentsError: String? = null,
    val commentText: String = "",
    val sendingComment: Boolean = false,
    val commentError: String? = null,
    val savingRating: Boolean = false,
    val sharing: Boolean = false,
    val deleting: Boolean = false,
    val deleted: Boolean = false,
    val actionError: String? = null,
)

@HiltViewModel
class EventViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val events: EventsRepository,
    private val tokens: TokenStore,
) : ViewModel() {
    private val eventId = checkNotNull(savedStateHandle.get<Long>("eventId"))
    private val _state = MutableStateFlow(EventUiState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null
    private var commentsJob: Job? = null
    private var photosJob: Job? = null
    private var summaryRequest = 0L

    fun load() {
        loadJob?.cancel()
        val request = ++summaryRequest
        _state.update { it.copy(loading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            events.getEvent(eventId).onSuccess { event ->
                if (request == summaryRequest) displayEvent(event)
                loadPhotos()
                loadComments(reset = true)
            }.onFailure { error ->
                if (request != summaryRequest) return@onFailure
                val gone = error is HttpException && error.code() == 404
                _state.update {
                    it.copy(event = if (gone) null else it.event, loading = false, loadError = error.eventError())
                }
            }
        }
    }

    fun loadPhotos() {
        photosJob?.cancel()
        photosJob = viewModelScope.launch {
            _state.update { it.copy(photosError = null) }
            events.getPhotos(eventId).onSuccess { photos ->
                _state.update { it.copy(photos = photos) }
            }.onFailure { error ->
                _state.update { it.copy(photosError = error.eventError()) }
            }
        }
    }

    fun loadComments(reset: Boolean = false) {
        if (!reset && _state.value.loadingComments) return
        val page = if (reset) 0 else _state.value.nextCommentPage ?: return
        commentsJob?.cancel()
        _state.update { it.copy(loadingComments = true, commentsError = null, nextCommentPage = page) }
        commentsJob = viewModelScope.launch {
            events.getComments(eventId, page).onSuccess { result ->
                _state.update {
                    val comments = if (reset || page == 0) result.content
                    else (it.comments + result.content).distinctBy { comment -> comment.id }
                    it.copy(
                        comments = comments,
                        nextCommentPage = if (result.last) null else page + 1,
                        loadingComments = false,
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(loadingComments = false, commentsError = error.eventError()) }
            }
        }
    }

    fun updateComment(text: String) {
        _state.update { if (it.sendingComment) it else it.copy(commentText = text, commentError = null) }
    }

    fun sendComment() {
        val current = _state.value
        if (current.sendingComment || current.event == null || current.deleting) return
        val comment = current.commentText.trim()
        val error = when {
            comment.isEmpty() -> "Enter a comment."
            comment.codePointCount(0, comment.length) > 100 -> "Use at most 100 characters."
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(commentError = error) }
            return
        }
        _state.update { it.copy(sendingComment = true, commentError = null) }
        viewModelScope.launch {
            events.addComment(eventId, comment).onSuccess {
                _state.update { it.copy(commentText = "", sendingComment = false) }
                loadComments(reset = true)
                refreshSummary()
            }.onFailure {
                _state.update { state -> state.copy(sendingComment = false, commentError = it.eventError()) }
            }
        }
    }

    fun rate(rating: Int) {
        if (_state.value.savingRating || _state.value.deleting || _state.value.event == null) return
        _state.update { it.copy(savingRating = true, actionError = null) }
        viewModelScope.launch {
            events.rate(eventId, rating).onSuccess {
                refreshSummary()
            }.onFailure { error ->
                _state.update { it.copy(actionError = error.eventError()) }
            }
            _state.update { it.copy(savingRating = false) }
        }
    }

    fun share() {
        if (_state.value.sharing || _state.value.deleting || _state.value.event == null) return
        _state.update { it.copy(sharing = true, actionError = null) }
        viewModelScope.launch {
            events.share(eventId).onSuccess {
                refreshSummary()
            }.onFailure { error ->
                _state.update { it.copy(actionError = error.eventError()) }
            }
            _state.update { it.copy(sharing = false) }
        }
    }

    fun delete() {
        val current = _state.value
        if (!current.canManage || current.deleting || current.event == null) return
        _state.update { it.copy(deleting = true, actionError = null) }
        viewModelScope.launch {
            events.deleteEvent(eventId).onSuccess {
                _state.update { it.copy(deleting = false, deleted = true) }
            }.onFailure { error ->
                _state.update { it.copy(deleting = false, actionError = error.eventError()) }
            }
        }
    }

    private suspend fun refreshSummary() {
        val request = ++summaryRequest
        val result = events.getEvent(eventId)
        if (request != summaryRequest) return
        result.onSuccess(::displayEvent).onFailure {
            _state.update { state -> state.copy(loading = false, loadError = "Could not refresh the event. Try again.") }
        }
    }

    private fun displayEvent(event: EventDto) {
        val user = tokens.session()
        _state.update {
            it.copy(
                event = event,
                canManage = user != null && (event.userId == user.userId || user.isAdmin),
                loading = false,
                loadError = null,
            )
        }
    }
}
