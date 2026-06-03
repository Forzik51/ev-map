package com.evmap.serverapp.features.chat.domian

import java.time.OffsetDateTime

data class ChatMessage(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val senderUsername: String?,
    val text: String,
    val sentAt: OffsetDateTime,
)
