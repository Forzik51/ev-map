package com.evmap.mobileapp.feature.explore.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.feature.events.data.EventsRepository
import com.evmap.mobileapp.feature.explore.data.SearchRepository
import com.evmap.mobileapp.feature.explore.data.toEventUi
import com.evmap.mobileapp.feature.explore.data.toProfileUserUi
import dagger.hilt.android.lifecycle.HiltViewModel
import feature.explore.ExploreFeature
import feature.explore.ExploreFilters
import feature.explore.ExploreSort
import feature.explore.ExploreTab
import feature.explore.ExploreUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: SearchRepository,
    private val eventsRepository: EventsRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(restoreState())
    val state = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        submitSearch()
    }

    fun updateQuery(query: String) {
        if (state.value.query == query) return
        savedStateHandle[STATE_QUERY] = query
        _state.update { it.copy(query = query) }
        search(delayMs = SEARCH_DEBOUNCE_MS)
    }

    fun submitSearch() = search()

    fun selectTab(tab: ExploreTab) {
        if (state.value.selectedTab == tab) return
        savedStateHandle[STATE_TAB] = tab.apiValue
        _state.update { it.copy(selectedTab = tab, draftFilters = null) }
        submitSearch()
    }

    fun openFilters() {
        _state.update { it.copy(draftFilters = it.filters) }
        loadCategories()
    }

    fun closeFilters() {
        _state.update { it.copy(draftFilters = null) }
    }

    fun updateSort(sort: ExploreSort) {
        _state.update { it.copy(draftFilters = it.draftFilters?.copy(sort = sort)) }
    }

    fun updateTags(value: String) { _state.update { it.copy(draftFilters=it.draftFilters?.copy(tagsText=value)) } }

    fun updateCategory(categoryId: Long?) {
        _state.update { it.copy(draftFilters = it.draftFilters?.copy(categoryId = categoryId)) }
    }

    fun applyFilters() {
        val filters = state.value.draftFilters ?: return
        savedStateHandle[STATE_SORT] = filters.sort.apiValue
        savedStateHandle[STATE_CATEGORY] = filters.categoryId
        savedStateHandle["exploreTags"] = filters.tagsText
        _state.update { it.copy(filters = filters, draftFilters = null) }
        submitSearch()
    }

    fun loadCategories() {
        if (state.value.loadingCategories || state.value.categories != null) return
        _state.update { it.copy(loadingCategories = true, categoryLoadFailed = false) }
        viewModelScope.launch {
            val categories = eventsRepository.getCategories().getOrNull()
            _state.update {
                it.copy(categories = categories, loadingCategories = false, categoryLoadFailed = categories == null)
            }
        }
    }

    private fun search(delayMs: Long = 0) {
        searchJob?.cancel()
        val request = state.value
        val query = request.query.trim()
        _state.update { it.copy(loading = query.isNotEmpty(), error = null, users = emptyList(), events = emptyList()) }
        if (query.isEmpty()) return

        searchJob = viewModelScope.launch {
            delay(delayMs)
            val result = repository.search(query, request.selectedTab, request.filters)
            currentCoroutineContext().ensureActive()
            result.fold(
                onSuccess = { response ->
                    _state.update {
                        it.copy(
                            loading = false,
                            users = response.users.map { user -> user.toProfileUserUi() },
                            events = response.events.map { event -> event.toEventUi() },
                        )
                    }
                },
                onFailure = { error -> _state.update { it.copy(loading = false, error = error.searchMessage()) } },
            )
        }
    }

    private fun restoreState(): ExploreUiState {
        val query = savedStateHandle.get<String>(STATE_QUERY)
            ?: ExploreFeature.decodeQuery(savedStateHandle[ExploreFeature.QUERY_ARGUMENT])
        val tab = ExploreTab.entries.find { it.apiValue == savedStateHandle.get<String>(STATE_TAB) }
            ?: if (query.trimStart().startsWith('@')) ExploreTab.USERS else ExploreTab.EVENTS
        val sort = ExploreSort.entries.find { it.apiValue == savedStateHandle.get<String>(STATE_SORT) }
            ?: ExploreSort.RELEVANCE
        return ExploreUiState(
            query = query,
            selectedTab = tab,
            filters = ExploreFilters(sort, savedStateHandle[STATE_CATEGORY], savedStateHandle.get<String>("exploreTags").orEmpty()),
        )
    }

    private fun Throwable.searchMessage(): String = when {
        this is IOException -> "Check your connection and try again."
        this is HttpException && code() == 401 -> "Sign in again to continue."
        else -> "Could not complete the search. Try again."
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 250L
        const val STATE_QUERY = "exploreQuery"
        const val STATE_TAB = "exploreTab"
        const val STATE_SORT = "exploreSort"
        const val STATE_CATEGORY = "exploreCategory"
    }
}
