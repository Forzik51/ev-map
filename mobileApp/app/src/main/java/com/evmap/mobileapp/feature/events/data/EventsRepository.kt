package com.evmap.mobileapp.feature.events.data

import com.evmap.mobileapp.core.data.dto.EventDto
import com.evmap.mobileapp.core.data.remote.EventsApi
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class EventsRepository @Inject constructor(
    private val api: EventsApi,
) {
    suspend fun getEvent(id: Long): Result<EventDto> = runCatching {
        api.getEvent(id)
    }
}