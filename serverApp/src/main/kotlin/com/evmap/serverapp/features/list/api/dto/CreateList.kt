package com.evmap.serverapp.features.list.api.dto

import jakarta.validation.constraints.Positive

data class CreateList(
    val name: String,

    @Deprecated("Ignored by the server. The authenticated user is used instead.")
    @field:Positive
    val userId: Long? = null,
)
