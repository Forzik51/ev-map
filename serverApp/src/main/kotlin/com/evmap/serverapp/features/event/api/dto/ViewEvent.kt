package com.evmap.serverapp.features.event.api.dto

data class ViewEvent(
    val id: Long,
    val name: String,
    val description: String,
    val startsAt: String,
    val locationName: String?
)