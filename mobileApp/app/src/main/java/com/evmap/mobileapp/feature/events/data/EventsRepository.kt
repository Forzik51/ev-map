package com.evmap.mobileapp.feature.events.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.evmap.mobileapp.core.data.dto.CreateEventRequest
import com.evmap.mobileapp.core.data.dto.EventDto
import com.evmap.mobileapp.core.data.dto.PageResponse
import com.evmap.mobileapp.core.data.remote.EventsApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import okhttp3.MultipartBody
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsRepository @Inject constructor(private val api: EventsApi) {
    private val eventChanges = MutableStateFlow(0L)

    fun feed() = pagedEvents(startFromFirstPage = true) { page, size -> api.getFeed(page = page, size = size) }
    fun catalog() = pagedEvents(loadPage = api::getEvents)
    fun byAuthor(userId: Long) = pagedEvents { page, size -> api.getUserEvents(userId, page, size) }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun pagedEvents(
        startFromFirstPage: Boolean = false,
        loadPage: suspend (Int, Int) -> PageResponse<EventDto>,
    ) = eventChanges.flatMapLatest {
        Pager(PagingConfig(pageSize = PAGE_SIZE, initialLoadSize = PAGE_SIZE)) {
            EventPagingSource(startFromFirstPage, loadPage)
        }.flow
    }

    suspend fun getEvent(id: Long) = call { api.getEvent(id) }
    suspend fun getCategories() = call { api.getCategories() }
    suspend fun getComments(eventId: Long, page: Int) = call { api.getComments(eventId, page) }
    suspend fun getPhotos(eventId: Long) = call { api.getPhotos(eventId) }

    suspend fun createEvent(request: CreateEventRequest) = change { api.createEvent(request) }
    suspend fun updateEvent(id: Long, request: CreateEventRequest) = change { api.updateEvent(id, request).requireSuccess() }
    suspend fun deleteEvent(id: Long) = change { api.deleteEvent(id).requireSuccess() }
    suspend fun addComment(id: Long, comment: String) = change { api.addComment(id, comment).requireSuccess() }
    suspend fun rate(id: Long, rating: Int) = change { api.addReaction(id, rating.toString()).requireSuccess() }
    suspend fun share(id: Long) = change { api.shareEvent(id).requireSuccess() }
    suspend fun uploadPhoto(eventId: Long, uploadId: String, file: MultipartBody.Part) =
        change { api.uploadPhoto(eventId, uploadId, file) }

    suspend fun orderPhotos(eventId: Long, ids: List<Long>) = change { api.orderPhotos(eventId,ids).requireSuccess() }

    suspend fun deletePhoto(eventId: Long, photoId: Long) = change {
        val response = api.deletePhoto(eventId, photoId)
        // A retry after the first DELETE was committed can find the photo already gone.
        if (response.code() != 404) response.requireSuccess()
    }

    private suspend fun <T> change(block: suspend () -> T): Result<T> =
        call(block).onSuccess { eventChanges.update { it + 1 } }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun Response<Unit>.requireSuccess() {
        if (!isSuccessful) throw HttpException(this)
    }
}

private const val PAGE_SIZE = 20

internal class EventPagingSource(
    private val startFromFirstPage: Boolean = false,
    private val loadPage: suspend (Int, Int) -> PageResponse<EventDto>,
) : PagingSource<Int, EventDto>() {
    private val feedEventIds = if (startFromFirstPage) mutableSetOf<Long>() else null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EventDto> = try {
        val page = params.key ?: 0
        val response = loadPage(page, PAGE_SIZE)
        // Ranked events can shift across offset pages between requests.
        val items = feedEventIds?.let { seen -> response.content.filter { seen.add(it.id) } } ?: response.content
        LoadResult.Page(
            data = items,
            prevKey = (page - 1).takeIf { page > 0 && !startFromFirstPage },
            nextKey = (page + 1).takeUnless { response.last },
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LoadResult.Error(e)
    }

    override fun getRefreshKey(state: PagingState<Int, EventDto>): Int? {
        if (startFromFirstPage) return null
        return state.anchorPosition?.let(state::closestPageToPosition)?.let { page ->
            page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
        }
    }
}
