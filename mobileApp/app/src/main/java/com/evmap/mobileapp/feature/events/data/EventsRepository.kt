package com.evmap.mobileapp.feature.events.data

import com.evmap.mobileapp.core.data.dto.CreateEventRequest
import com.evmap.mobileapp.core.data.dto.EventCategoryDto
import com.evmap.mobileapp.core.data.dto.EventCommentDto
import com.evmap.mobileapp.core.data.dto.EventDto
import com.evmap.mobileapp.core.data.dto.EventPhotoDto
import com.evmap.mobileapp.core.data.dto.EventReactionDto
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

    suspend fun getCategories(): Result<List<EventCategoryDto>> = runCatching {
        api.getCategories()
    }

    suspend fun createEvent(request: CreateEventRequest): Result<Long> = runCatching {
        api.createEvent(request)
    }

    suspend fun getComments(eventId: Long): Result<List<EventCommentDto>> = runCatching {
        api.getComments(eventId).content
    }

    suspend fun addComment(eventId: Long, comment: String): Result<Unit> = runCatching {
        api.addComment(eventId, comment)
        Unit
    }

    suspend fun getReactions(eventId: Long): Result<List<EventReactionDto>> = runCatching {
        api.getReactions(eventId).content
    }

    suspend fun addReaction(eventId: Long, reaction: String): Result<Unit> = runCatching {
        api.addReaction(eventId, reaction)
        Unit
    }

    suspend fun share(eventId: Long): Result<Unit> = runCatching {
        api.shareEvent(eventId)
        Unit
    }

    suspend fun getPhotos(eventId: Long): Result<List<EventPhotoDto>> = runCatching {
        api.getPhotos(eventId)
    }
}
