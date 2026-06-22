package com.evmap.mobileapp.feature.chat.data

import com.evmap.mobileapp.core.data.dto.ChatDto
import com.evmap.mobileapp.core.data.dto.MessageDto
import com.evmap.mobileapp.core.data.dto.SendMessageRequest
import com.evmap.mobileapp.core.data.remote.ChatsApi
import com.evmap.mobileapp.core.data.session.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatsApi: ChatsApi,
    private val tokenStore: TokenStore,
) {
    fun currentUserId(): Long? = tokenStore.session()?.userId?.takeIf { it > 0 }

    suspend fun chats(): Result<List<ChatDto>> = runCatching {
        chatsApi.mine()
    }

    suspend fun chat(chatId: Long): Result<ChatDto> = runCatching {
        chatsApi.info(chatId)
    }

    suspend fun messages(chatId: Long): Result<List<MessageDto>> = runCatching {
        chatsApi.messages(chatId = chatId, limit = 100)
    }

    suspend fun send(chatId: Long, text: String): Result<MessageDto> = runCatching {
        chatsApi.send(chatId, SendMessageRequest(text))
    }

    suspend fun delete(chatId: Long): Result<Unit> = runCatching {
        chatsApi.delete(chatId)
        Unit
    }
}
