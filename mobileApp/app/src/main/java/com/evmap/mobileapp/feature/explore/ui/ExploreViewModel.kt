package com.evmap.mobileapp.feature.explore.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.remote.toUserMessage
import com.evmap.mobileapp.feature.explore.data.SearchRepository
import com.evmap.mobileapp.feature.explore.data.toEventUi
import com.evmap.mobileapp.feature.explore.data.toProfileUserUi
import dagger.hilt.android.lifecycle.HiltViewModel
import feature.explore.ExploreTab
import feature.explore.ExploreUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: SearchRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ExploreUiState(isFilterOpen = false))
    val state: StateFlow<ExploreUiState> = _state

    private var searchJob: Job? = null

    fun updateQuery(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(users = emptyList(), events = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            repository.search(query)
                .onSuccess { response ->
                    _state.update {
                        it.copy(
                            users = response.users.map { user -> user.toProfileUserUi() },
                            events = response.events.map { event -> event.toEventUi() },
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(query = query, users = emptyList(), events = emptyList()) }
                    android.util.Log.e("ExploreViewModel", error.toUserMessage(), error)
                }
        }
    }

    fun selectTab(tab: ExploreTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun openFilters() {
        _state.update { it.copy(isFilterOpen = true) }
    }

    fun closeFilters() {
        _state.update { it.copy(isFilterOpen = false) }
    }

    fun applyFilters() {
        _state.update { it.copy(isFilterOpen = false) }
        if (_state.value.query.isNotBlank()) updateQuery(_state.value.query)
    }

    fun updateSortBy(sortBy: String) {
        _state.update { it.copy(sortBy = sortBy) }
    }

    fun updateSelectedType(type: String) {
        _state.update { it.copy(selectedType = type) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
