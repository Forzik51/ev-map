package com.evmap.serverapp.features.report.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateReport(
    @field:NotBlank
    @field:Size(max = 100)
    val description: String,

    @field:Positive
    val eventId: Long,

    @field:Positive
    val categoryId: Long,

    @field:Positive
    val userId: Long,
)
