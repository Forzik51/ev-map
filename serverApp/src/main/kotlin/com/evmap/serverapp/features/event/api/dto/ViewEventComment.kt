package com.evmap.serverapp.features.event.api.dto

data class ViewEventComment(
    val id: Long,
    val text: String,
    val eventId: Long,
    val parentCommentId: Long?,
    val commentAt: String,
    val userId: Long,
    val username: String?,
)
