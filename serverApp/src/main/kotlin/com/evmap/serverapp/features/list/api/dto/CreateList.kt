package com.evmap.serverapp.features.list.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateList(
    @field:NotBlank
    @field:Size(max = 20)
    val name: String,

    @Deprecated("Ignored by the server. The authenticated user is used instead.")
    @field:Positive
    val userId: Long? = null,
)
