package com.evmap.serverapp.features.event.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateEvent(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:NotBlank
    @field:Size(max = 2000)
    val description: String,

    val startsAt: Instant? = null,

    val endsAt: Instant? = null,

    @field:Positive
    val locationId: Long,

    @Deprecated("Ignored by the server. The authenticated user is used instead.")
    @field:Positive
    val userId: Long? = null,

    @field:NotEmpty
    @field:Size(max = 3)
    val categoryIds: List<@Positive Long>,
    val dateMode: String = if (startsAt == null) "UNSPECIFIED" else "DATETIME",
    val startsOn: java.time.LocalDate? = null,
    val endsOn: java.time.LocalDate? = null,
    val timeZone: String = "UTC",
    @field:Size(max = 5) val tags: List<String> = emptyList(),
    val clientRequestId: java.util.UUID? = null,
) {
    @get:AssertTrue(message = "End time must not be before the start time")
    val isTimeRangeValid: Boolean
        get() = endsAt == null || (startsAt != null && !endsAt.isBefore(startsAt))
}
