package com.evmap.mobileapp.feature.explore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.evmap.mobileapp.core.data.dto.EventCategoryDto
import com.evmap.mobileapp.core.data.dto.SearchEventDto
import com.evmap.mobileapp.core.data.dto.SearchResponseDto
import com.evmap.mobileapp.core.data.dto.SearchSuggestionDto
import com.evmap.mobileapp.core.data.remote.EventsApi
import com.evmap.mobileapp.core.data.remote.SearchApi
import com.evmap.mobileapp.feature.events.data.EventsRepository
import com.evmap.mobileapp.feature.explore.data.SearchRepository
import com.evmap.mobileapp.feature.explore.ui.ExploreViewModel
import feature.explore.ExploreFeature
import feature.explore.ExploreSort
import feature.explore.ExploreTab
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ExploreViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val api = FakeSearchApi()
    private var categoryAttempts = 0
    private var failCategories = false
    private lateinit var viewModel: ExploreViewModel

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test fun `navigation preserves query and tabs and filters send supported parameters`() = runTest(dispatcher) {
        val query = "музика & # %2F + ?"
        viewModel = createViewModel(query)
        runCurrent()
        assertEquals(query, viewModel.state.value.query)
        assertEquals(listOf(SearchRequest(query, "events", "relevance", null)), api.requests)

        viewModel.openFilters()
        runCurrent()
        assertEquals(listOf(EventCategoryDto(7, "Music")), viewModel.state.value.categories)
        viewModel.updateCategory(7)
        viewModel.updateSort(ExploreSort.DATE)
        viewModel.closeFilters()
        runCurrent()
        assertEquals(1, api.requests.size)

        viewModel.openFilters()
        assertEquals(ExploreSort.RELEVANCE, viewModel.state.value.draftFilters?.sort)
        assertNull(viewModel.state.value.draftFilters?.categoryId)
        viewModel.updateCategory(7)
        viewModel.updateSort(ExploreSort.RATING)
        viewModel.applyFilters()
        runCurrent()
        val filtered = SearchRequest(query, "events", "rating", listOf(7))
        assertEquals(filtered, api.requests.last())

        viewModel.selectTab(ExploreTab.USERS)
        runCurrent()
        assertEquals(SearchRequest(query, "users", null, null), api.requests.last())
        viewModel.selectTab(ExploreTab.EVENTS)
        runCurrent()
        assertEquals(filtered, api.requests.last())
        assertEquals(1, categoryAttempts)
    }

    @Test fun `typing debounces and an old response cannot replace the latest results`() = runTest(dispatcher) {
        val oldResponse = CompletableDeferred<SearchResponseDto>()
        api.respond = { query ->
            if (query == "Old") withContext(NonCancellable) { oldResponse.await() } else eventResult(query)
        }
        viewModel = createViewModel()
        runCurrent()
        assertTrue(api.requests.isEmpty())
        viewModel.updateQuery("O")
        advanceTimeBy(100)
        viewModel.updateQuery("Old")
        advanceTimeBy(249)
        runCurrent()
        assertTrue(api.requests.isEmpty())
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("Old"), api.requests.map { it.query })

        viewModel.updateQuery("  Latest  ")
        viewModel.submitSearch()
        runCurrent()
        assertEquals(listOf("Old", "Latest"), api.requests.map { it.query })
        oldResponse.complete(eventResult("Old"))
        runCurrent()
        assertEquals("Latest", viewModel.state.value.events.single().title)
        assertFalse(viewModel.state.value.loading)
        assertNull(viewModel.state.value.error)
    }

    @Test fun `initial username search and category loading can be retried after failure`() = runTest(dispatcher) {
        api.respond = { throw IOException("Offline") }
        viewModel = createViewModel("@alice")
        runCurrent()
        assertEquals(SearchRequest("@alice", "users", null, null), api.requests.single())
        assertEquals("Check your connection and try again.", viewModel.state.value.error)
        api.respond = { SearchResponseDto(type = "users", limit = 30) }
        viewModel.submitSearch()
        runCurrent()
        assertEquals(api.requests.first(), api.requests.last())
        assertNull(viewModel.state.value.error)

        failCategories = true
        viewModel.selectTab(ExploreTab.EVENTS)
        viewModel.openFilters()
        runCurrent()
        assertTrue(viewModel.state.value.categoryLoadFailed)
        failCategories = false
        viewModel.loadCategories()
        runCurrent()
        assertFalse(viewModel.state.value.categoryLoadFailed)
        assertEquals(7L, viewModel.state.value.categories?.single()?.id)
    }

    private fun createViewModel(query: String = ""): ExploreViewModel {
        val encoded = ExploreFeature.routeForQuery(query).substringAfter('=', "")
        val unusedApi = Retrofit.Builder().baseUrl("http://localhost/").build().create(EventsApi::class.java)
        val eventsApi = object : EventsApi by unusedApi {
            override suspend fun getCategories(): List<EventCategoryDto> {
                categoryAttempts++
                if (failCategories) throw IOException("Offline")
                return listOf(EventCategoryDto(7, "Music"))
            }
        }
        return ExploreViewModel(
            SearchRepository(api),
            EventsRepository(eventsApi),
            SavedStateHandle(mapOf(ExploreFeature.QUERY_ARGUMENT to encoded)),
        )
    }

    private fun eventResult(name: String) = SearchResponseDto(
        type = "events",
        limit = 30,
        events = listOf(SearchEventDto(1, name, "Description", null, "2030-03-22T18:30:00Z", 0.0, 0, null, emptyList())),
    )

    private data class SearchRequest(val query: String, val type: String?, val sort: String?, val categories: List<Long>?)

    private class FakeSearchApi : SearchApi {
        val requests = mutableListOf<SearchRequest>()
        var respond: suspend (String) -> SearchResponseDto = { SearchResponseDto(type = "events", limit = 30) }

        override suspend fun search(
            query: String, type: String?, lat: Double?, lng: Double?, limit: Int?, sort: String?, categoryIds: List<Long>?, tags: List<String>?,
        ): SearchResponseDto {
            requests += SearchRequest(query, type, sort, categoryIds)
            return respond(query)
        }

        override suspend fun suggestions(query: String, limit: Int?): List<SearchSuggestionDto> = emptyList()
        override suspend fun popular(limit: Int?): List<SearchSuggestionDto> = emptyList()
    }
}
