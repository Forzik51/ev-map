package com.evmap.serverapp.features.event.api.dto

data class ViewEvent(
    val id: Long,
    val name: String,
    val description: String,
    val startsAt: String,
    val endsAt: String?,
    val locationName: String?,
    val userId: Long,
    val repostsCount: Int,
    val ratingsCount: Int,
    val commentsCount: Int,
)
