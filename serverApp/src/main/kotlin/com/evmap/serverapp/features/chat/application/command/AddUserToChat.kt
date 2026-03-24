package com.evmap.serverapp.features.chat.application.command

import com.evmap.serverapp.features.chat.domian.ChatRepositoryPort
import org.springframework.stereotype.Service

@Service
class AddUserToChat(
    private val repo: ChatRepositoryPort,
) {
    fun handle(chatId: Long, userId: Long) {
        repo.addUser(chatId, userId)
    }
}
