package com.evmap.serverapp.features.event.api.dto

import jakarta.validation.constraints.NotBlank
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

    @field:Positive
    val userId: Long,
)
