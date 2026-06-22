package com.evmap.mobileapp.core.data.dto

data class CreateChatRequest(
    val name: String,
    val participantUserIds: List<Long> = emptyList(),
)

data class ChatDto(
    val id: Long,
    val name: String,
    val usersCount: Int,
    val lastMessageAt: String?,
    val userIds: List<Long>,
)

data class SendMessageRequest(
    val text: String,
)

data class MessageDto(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val senderUsername: String?,
    val text: String,
    val sentAt: String,
)

data class MarkChatReadRequest(
    val lastReadMessageId: Long,
)
