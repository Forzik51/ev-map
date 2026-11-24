package com.evmap.serverapp.features.chat.api.dto

data class TypingEventRequest(
    val typing: Boolean = true,
)

data class ViewTypingEvent(
    val chatId: Long,
    val userId: Long,
    val username: String?,
    val typing: Boolean,
)

data class ViewReadEvent(
    val chatId: Long,
    val userId: Long,
    val lastReadMessageId: Long,
    val lastReadSentAt: String,
)
