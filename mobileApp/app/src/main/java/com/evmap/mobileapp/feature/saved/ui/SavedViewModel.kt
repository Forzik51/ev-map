package com.evmap.mobileapp.feature.saved.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.feature.feed.data.toEventUi
import com.evmap.mobileapp.feature.saved.data.ListsRepository
import com.evmap.mobileapp.feature.saved.data.toSavedGroupUi
import core.ui.model.GroupItemsUi
import core.ui.model.SavedGroupUi
import core.ui.model.ViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

enum class SavedDialog { NAME, DELETE }

data class SavedUiState(
    val groups: List<SavedGroupUi> = emptyList(),
    val groupItems: GroupItemsUi? = null,
    val loading: Boolean = true,
    val loadError: String? = null,
    val writing: Boolean = false,
    val writeError: String? = null,
    val dialog: SavedDialog? = null,
    val nameDraft: String = "",
    val deleted: Boolean = false,
    val savedTo: String? = null,
) {
    val busy: Boolean get() = loading || writing
}

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val repository: ListsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val groupId = savedStateHandle.get<Long>("groupId")
    private val eventId = savedStateHandle.get<Long>("eventId")
    private val _state = MutableStateFlow(SavedUiState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    fun load() {
        if (loadJob?.isActive == true || state.value.writing || state.value.deleted || state.value.savedTo != null) return
        _state.update { it.copy(loading = true, loadError = null) }
        loadJob = viewModelScope.launch {
            if (groupId == null) {
                repository.mine().onSuccess { lists ->
                    _state.update { it.copy(groups = lists.map { list -> list.toSavedGroupUi() }, loading = false) }
                }.onFailure(::loadFailed)
            } else {
                repository.details(groupId).onSuccess { details ->
                    _state.update { current ->
                        current.copy(
                            groupItems = GroupItemsUi(
                                groupId = details.list.id.toString(),
                                title = details.list.name,
                                items = details.events.map { it.toEventUi() },
                                viewMode = current.groupItems?.viewMode ?: ViewMode.LIST,
                            ),
                            loading = false,
                        )
                    }
                }.onFailure(::loadFailed)
            }
        }
    }

    private fun loadFailed(error: Throwable) {
        val unavailable = error is HttpException && (error.code() == 404 || error.code() == 403)
        _state.update { it.copy(loading = false, loadError = error.savedMessage(), groupItems = it.groupItems.takeUnless { unavailable }) }
    }

    fun openNameDialog() {
        if (state.value.busy || groupId != null && state.value.groupItems == null) return
        _state.update { it.copy(dialog = SavedDialog.NAME, nameDraft = it.groupItems?.title.orEmpty(), writeError = null) }
    }

    fun updateName(value: String) {
        _state.update { if (it.writing) it else it.copy(nameDraft = value, writeError = null) }
    }

    fun dismissDialog() {
        _state.update { if (it.writing) it else it.copy(dialog = null, writeError = null) }
    }

    fun saveName() {
        val current = state.value
        if (current.dialog != SavedDialog.NAME || current.busy) return
        val name = current.nameDraft.trim()
        if (name.length !in 1..20) {
            _state.update { it.copy(writeError = "Enter a list name from 1 to 20 characters") }
            return
        }
        if (groupId == null) {
            mutate({ repository.create(name) }) { state, id ->
                state.copy(groups = listOf(SavedGroupUi(id.toString(), name)) + state.groups, dialog = null)
            }
        } else {
            mutate({ repository.rename(groupId, name) }) { state, _ ->
                state.copy(groupItems = state.groupItems?.copy(title = name), dialog = null)
            }
        }
    }

    fun confirmDelete() {
        if (state.value.busy || state.value.groupItems == null) return
        _state.update { it.copy(dialog = SavedDialog.DELETE, writeError = null) }
    }

    fun deleteList() {
        val id = groupId ?: return
        if (state.value.dialog != SavedDialog.DELETE) return
        mutate({ repository.delete(id) }) { state, _ -> state.copy(dialog = null, deleted = true) }
    }

    fun saveEvent(list: SavedGroupUi) {
        val event = eventId ?: return
        val id = list.id.toLongOrNull() ?: return
        mutate({ repository.addEvent(id, event) }) { state, _ -> state.copy(savedTo = list.name) }
    }

    fun removeEvent(rawId: String) {
        val id = groupId ?: return
        val event = rawId.toLongOrNull() ?: return
        mutate({ repository.removeEvent(id, event) }) { state, _ ->
            state.copy(groupItems = state.groupItems?.let { it.copy(items = it.items.filterNot { item -> item.id == rawId }) })
        }
    }

    fun toggleView() {
        _state.update { state ->
            val group = state.groupItems ?: return@update state
            state.copy(groupItems = group.copy(viewMode = if (group.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST))
        }
    }

    private fun <T> mutate(request: suspend () -> Result<T>, apply: (SavedUiState, T) -> SavedUiState) {
        if (state.value.busy || state.value.deleted || state.value.savedTo != null) return
        _state.update { it.copy(writing = true, writeError = null) }
        viewModelScope.launch {
            request().onSuccess { result ->
                _state.update { apply(it, result).copy(writing = false) }
            }.onFailure { error ->
                _state.update { it.copy(writing = false, writeError = error.savedMessage()) }
            }
        }
    }
}

private fun Throwable.savedMessage(): String = when (this) {
    is HttpException -> when (code()) {
        401 -> "Sign in again to continue."
        403 -> "You do not have access to this list."
        404 -> "This list or event is no longer available."
        400 -> runCatching { response()?.errorBody()?.string()?.let { JSONObject(it).optString("message") } }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: "Check the entered data."
        else -> "The server could not complete the request. Try again."
    }
    is IOException -> "Check your connection and try again."
    else -> "Could not complete the request. Try again."
}
