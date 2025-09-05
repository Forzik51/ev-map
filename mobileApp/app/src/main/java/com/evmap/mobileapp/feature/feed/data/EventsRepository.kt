package com.evmap.mobileapp.feature.feed.data

import androidx.paging.*
import com.evmap.mobileapp.core.data.dto.EventDto
import com.evmap.mobileapp.core.data.remote.EventsApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

class EventsPagingSource(
    private val api: EventsApi
) : PagingSource<Int, EventDto>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EventDto> = try {
        val page = params.key ?: 0
        val res = api.getEvents(page = page, size = params.loadSize)
        LoadResult.Page(res.content, if (page==0) null else page-1, if (res.last) null else page+1)
    } catch (t: Throwable) { LoadResult.Error(t) }

    override fun getRefreshKey(state: PagingState<Int, EventDto>) = null
}

@Singleton
class EventsRepository @Inject constructor(
    private val api: EventsApi
) {
    fun feed(): Flow<PagingData<EventDto>> = Pager(
        PagingConfig(pageSize = 20, prefetchDistance = 5)
    ) { EventsPagingSource(api) }.flow
}


//@HiltViewModel
//class FeedViewModel @Inject constructor(
//    private val repo: EventsRepository
//) : ViewModel() {
//    val feed = repo.feed().cachedIn(viewModelScope)
//}