package com.evmap.mobileapp.core.data.dto

data class EventDto(
    val id: Long,
    val name: String,
    val description: String,
    val startsAt: String,
    val endsAt: String? = null,
    val locationName: String? = null,
    val imagePath: String? = null,
    val userId: Long,
    val repostsCount: Int = 0,
    val ratingsCount: Int = 0,
    val commentsCount: Int = 0,
    val categories: List<EventCategoryDto> = emptyList(),
)

data class CreateEventRequest(
    val name: String,
    val description: String,
    val startsAt: String,
    val endsAt: String? = null,
    val locationId: Long,
    val categoryIds: List<Long>,
)

data class EventCategoryDto(
    val id: Long,
    val name: String,
)

data class EventCommentDto(
    val id: Long,
    val text: String,
    val eventId: Long,
    val parentCommentId: Long?,
    val commentAt: String,
    val userId: Long,
    val username: String?,
)

data class EventReactionDto(
    val eventId: Long,
    val userId: Long,
    val username: String?,
    val rating: Double,
)

data class EventPhotoDto(
    val id: Long,
    val eventId: Long,
    val imagePath: String,
)
