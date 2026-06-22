package com.evmap.mobileapp.feature.saved.data

import com.evmap.mobileapp.core.data.dto.EventDto
import com.evmap.mobileapp.core.data.dto.ListDto
import com.evmap.mobileapp.core.data.remote.EventsApi
import com.evmap.mobileapp.core.data.remote.ListsApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListsRepository @Inject constructor(
    private val listsApi: ListsApi,
    private val eventsApi: EventsApi,
) {
    suspend fun mine(): Result<List<ListDto>> = runCatching {
        listsApi.mine()
    }

    suspend fun create(name: String): Result<Long> = runCatching {
        listsApi.create(com.evmap.mobileapp.core.data.dto.CreateListRequest(name))
    }

    suspend fun events(listId: Long): Result<List<EventDto>> = runCatching {
        listsApi.eventIds(listId).map { eventId -> eventsApi.getEvent(eventId) }
    }
}
