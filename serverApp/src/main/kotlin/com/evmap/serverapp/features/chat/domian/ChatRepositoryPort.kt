package com.evmap.serverapp.features.chat.domian

interface ChatRepositoryPort {
    fun save(chat: Chat, creatorUserId: Long, participantUserIds: List<Long>): Long
    fun addUser(chatId: Long, userId: Long)
    fun removeUser(chatId: Long, userId: Long)
    fun changeName(chatId: Long, name: String)
    fun delete(chatId: Long)
    fun sendMessage(chatId: Long, userId: Long, message: String)
}
