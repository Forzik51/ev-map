package com.evmap.serverapp.features.list.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateList(
    @field:NotBlank
    @field:Size(max = 20)
    val name: String,

    @field:Positive
    val userId: Long,
)
