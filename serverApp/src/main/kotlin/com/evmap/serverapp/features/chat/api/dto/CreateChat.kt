package com.evmap.serverapp.features.chat.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateChat(
    @field:NotBlank
    @field:Size(max = 20)
    val name: String,

    @Deprecated("Ignored by the server. The authenticated user is used instead.")
    @field:Positive
    val creatorUserId: Long? = null,

    val participantUserIds: List<Long> = emptyList(),
)
