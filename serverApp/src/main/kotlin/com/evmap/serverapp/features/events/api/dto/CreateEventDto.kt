package com.evmap.serverapp.features.events.api.dto

data class CreateEventDto(
    val name: String,
    val description: String,
    val startsAt: String,
    val locationId: Long
)