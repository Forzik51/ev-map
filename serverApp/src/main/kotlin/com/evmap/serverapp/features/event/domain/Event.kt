package com.evmap.serverapp.features.event.domain

data class Event(
    val id: Long? = null,
    val name: String,
    val description: String,
    val startsAt: java.time.Instant?,
    val endsAt: java.time.Instant? = null,
    val locationId: Long,
    val userId: Long,
    val categoryIds: List<Long>,
    val createdAt: java.time.Instant = java.time.Instant.now(),
    val dateMode: String = "DATETIME",
    val startsOn: java.time.LocalDate? = null,
    val endsOn: java.time.LocalDate? = null,
    val timeZone: String = "UTC",
    val tags: List<String> = emptyList(),
)
