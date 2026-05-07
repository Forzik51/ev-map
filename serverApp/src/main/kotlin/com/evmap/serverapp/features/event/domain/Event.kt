package com.evmap.serverapp.features.event.domain

data class Event(
    val id: Long? = null,
    val name: String,
    val description: String,
    val startsAt: java.time.Instant,
    val endsAt: java.time.Instant? = null,
    val locationId: Long,
    val userId: Long,
    val categoryIds: List<Long>,
    val createdAt: java.time.Instant = java.time.Instant.now(),
)
