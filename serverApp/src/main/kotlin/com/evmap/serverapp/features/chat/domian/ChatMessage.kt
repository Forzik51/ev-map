package com.evmap.serverapp.features.chat.domian

import com.evmap.serverapp.features.chat.api.dto.ViewAttachment
import java.time.OffsetDateTime

data class ChatMessage(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val senderUsername: String?,
    val text: String,
    val sentAt: OffsetDateTime,
    val attachment: ViewAttachment? = null,
    val created: Boolean = true,
)

data class ChatAttachment(
    val kind: String,
    val name: String,
    val contentType: String,
    val bytes: ByteArray,
    val durationMs: Long?,
) {
    fun view(messageId: Long) = ViewAttachment(messageId, kind, name, contentType, bytes.size, durationMs)
}
