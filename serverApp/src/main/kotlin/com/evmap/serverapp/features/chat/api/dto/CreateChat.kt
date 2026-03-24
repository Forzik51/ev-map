package com.evmap.serverapp.features.chat.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class CreateChat(
    @field:NotBlank
    @field:Size(max = 20)
    val name: String,

    @field:Positive
    val creatorUserId: Long,

    val participantUserIds: List<Long> = emptyList(),
)
