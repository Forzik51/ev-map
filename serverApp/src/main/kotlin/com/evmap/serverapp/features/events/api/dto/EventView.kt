package com.evmap.serverapp.features.events.api.dto

data class EventView(
    val id: Long,
    val name: String,
    val description: String,
    val startsAt: String,
    val locationName: String?
)