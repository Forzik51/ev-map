package com.evmap.serverapp.features.events.domain

data class Event(
    val id: Long? = null,
    val name: String,
    val description: String,
    val startsAt: java.time.Instant,
    val locationId: Long
)