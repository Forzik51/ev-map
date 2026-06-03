package com.evmap.serverapp.features.chat.application.command

import com.evmap.serverapp.features.chat.domian.ChatRepositoryPort
import org.springframework.stereotype.Service

@Service
class MarkChatRead(
    private val repo: ChatRepositoryPort,
) {
    fun handle(chatId: Long, userId: Long, lastReadMessageId: Long) {
        repo.markRead(chatId, userId, lastReadMessageId)
    }
}
