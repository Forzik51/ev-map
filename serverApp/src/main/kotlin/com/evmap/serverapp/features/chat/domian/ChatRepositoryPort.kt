package com.evmap.serverapp.features.chat.domian

import java.time.OffsetDateTime
import java.util.UUID

interface ChatRepositoryPort {
    fun save(chat: Chat, creatorUserId: Long, participantUserIds: List<Long>): Long
    fun openDirect(userId: Long, peerId: Long): Long
    fun addUser(chatId: Long, userId: Long)
    fun removeUser(chatId: Long, userId: Long)
    fun changeName(chatId: Long, name: String)
    fun delete(chatId: Long)
    fun sendMessage(chatId: Long, userId: Long, message: String, clientMessageId: UUID?, attachment: ChatAttachment?): ChatMessage
    fun markRead(chatId: Long, userId: Long, lastReadMessageId: Long): OffsetDateTime?
    fun isOwner(chatId: Long, userId: Long): Boolean
}
