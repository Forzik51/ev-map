package com.evmap.serverapp.features.event.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.Instant

data class CreateEvent(
    @field:NotBlank
    @field:Size(max = 30)
    val name: String,

    @field:NotBlank
    @field:Size(max = 500)
    val description: String,

    val startsAt: Instant,

    val endsAt: Instant? = null,

    @field:Positive
    val locationId: Long,

    @Deprecated("Ignored by the server. The authenticated user is used instead.")
    @field:Positive
    val userId: Long? = null,

    @field:NotEmpty
    val categoryIds: List<@Positive Long>,
) {
    @get:AssertTrue(message = "End time must not be before the start time")
    val isTimeRangeValid: Boolean
        get() = endsAt == null || !endsAt.isBefore(startsAt)
}
