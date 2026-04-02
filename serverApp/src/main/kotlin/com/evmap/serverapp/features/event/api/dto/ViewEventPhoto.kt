package com.evmap.serverapp.features.event.api.dto

data class ViewEventPhoto(
    val id: Long,
    val eventId: Long,
    val imagePath: String,
)
