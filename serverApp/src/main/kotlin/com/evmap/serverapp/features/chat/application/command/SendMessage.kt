package com.evmap.serverapp.features.chat.application.command

import com.evmap.serverapp.features.chat.domian.ChatRepositoryPort
import org.springframework.stereotype.Service

@Service
class SendMessage(
    private val repo: ChatRepositoryPort,
) {
    fun handle(chatId: Long, userId: Long, message: String) {
        repo.sendMessage(chatId, userId, message)
    }
}
