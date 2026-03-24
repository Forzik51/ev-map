package com.evmap.serverapp.features.chat.application.command

import com.evmap.serverapp.features.chat.domian.ChatRepositoryPort
import org.springframework.stereotype.Service

@Service
class ChangeChatName(
    private val repo: ChatRepositoryPort,
) {
    fun handle(chatId: Long, name: String) {
        repo.changeName(chatId, name)
    }
}
