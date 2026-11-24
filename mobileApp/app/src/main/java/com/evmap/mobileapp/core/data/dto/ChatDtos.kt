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
    val lastMessageText: String? = null,
    val lastAttachmentKind: String? = null,
    val unreadCount: Int = 0,
)

data class SendMessageRequest(
    val text: String,
    val clientMessageId: String? = null,
)

data class MessageDto(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val senderUsername: String?,
    val text: String,
    val sentAt: String,
    val attachment: ChatAttachmentDto? = null,
)

data class ChatAttachmentDto(
    val id: Long,
    val kind: String,
    val name: String,
    val contentType: String,
    val sizeBytes: Int,
    val durationMs: Long? = null,
)

data class MarkChatReadRequest(
    val lastReadMessageId: Long,
)

data class ChatReadDto(val chatId: Long, val userId: Long, val lastReadMessageId: Long, val lastReadSentAt: String)
data class ChatTypingDto(val chatId: Long, val userId: Long, val username: String?, val typing: Boolean)
