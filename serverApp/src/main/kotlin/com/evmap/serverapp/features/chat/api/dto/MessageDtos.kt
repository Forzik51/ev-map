package com.evmap.serverapp.features.chat.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.util.UUID

data class SendMessageRequest(
    @field:NotBlank
    @field:Size(max = 5000)
    val text: String,
    val clientMessageId: UUID? = null,
)

data class ViewMessage(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val senderUsername: String?,
    val text: String,
    val sentAt: String,
    val attachment: ViewAttachment? = null,
)

data class ViewAttachment(
    val id: Long,
    val kind: String,
    val name: String,
    val contentType: String,
    val sizeBytes: Int,
    val durationMs: Long?,
)

data class MarkChatReadRequest(
    @field:Positive
    val lastReadMessageId: Long,
)
