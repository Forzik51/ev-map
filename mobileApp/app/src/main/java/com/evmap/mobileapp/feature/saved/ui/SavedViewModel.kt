package com.evmap.mobileapp.feature.saved.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.feed.data.toEventUi
import com.evmap.mobileapp.feature.saved.data.ListsRepository
import com.evmap.mobileapp.feature.saved.data.toSavedGroupUi
import core.ui.model.GroupItemsUi
import core.ui.model.SavedGroupUi
import core.ui.model.ViewMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedUiState(
    val groups: List<SavedGroupUi> = emptyList(),
    val groupItems: GroupItemsUi? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val repository: ListsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SavedUiState(loading = true))
    val state: StateFlow<SavedUiState> = _state

    fun loadGroups() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        repository.mine()
            .onSuccess { lists ->
                _state.update { it.copy(groups = lists.map { list -> list.toSavedGroupUi() }, loading = false) }
            }
            .onFailure { error ->
                _state.update { it.copy(loading = false, error = error.toUserMessage()) }
            }
    }

    fun loadGroup(rawGroupId: String) = viewModelScope.launch {
        val groupId = rawGroupId.toLongOrNull() ?: return@launch
        _state.update { it.copy(loading = true, error = null) }
        val group = _state.value.groups.firstOrNull { it.id == rawGroupId }
        repository.events(groupId)
            .onSuccess { events ->
                _state.update {
                    it.copy(
                        groupItems = GroupItemsUi(
                            groupId = rawGroupId,
                            title = group?.name ?: "Saved",
                            items = events.map { event -> event.toEventUi() },
                            viewMode = it.groupItems?.viewMode ?: ViewMode.LIST,
                        ),
                        loading = false,
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(loading = false, error = error.toUserMessage()) }
            }
    }

    fun toggleView() {
        _state.update { state ->
            state.copy(
                groupItems = state.groupItems?.copy(
                    viewMode = if (state.groupItems.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
                )
            )
        }
    }
}
