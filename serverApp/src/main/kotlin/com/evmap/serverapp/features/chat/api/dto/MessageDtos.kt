package com.evmap.serverapp.features.chat.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class SendMessageRequest(
    @field:NotBlank
    @field:Size(max = 5000)
    val text: String,
)

data class ViewMessage(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val senderUsername: String?,
    val text: String,
    val sentAt: String,
)

data class MarkChatReadRequest(
    @field:Positive
    val lastReadMessageId: Long,
)
