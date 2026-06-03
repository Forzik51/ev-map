package com.evmap.serverapp.features.chat.application.command

import com.evmap.serverapp.features.chat.api.dto.ViewMessage
import com.evmap.serverapp.features.chat.domian.ChatRepositoryPort
import org.springframework.stereotype.Service

@Service
class SendMessage(
    private val repo: ChatRepositoryPort,
) {
    fun handle(chatId: Long, userId: Long, message: String): ViewMessage =
        repo.sendMessage(chatId, userId, message).let {
            ViewMessage(
                id = it.id,
                chatId = it.chatId,
                senderId = it.senderId,
                senderUsername = it.senderUsername,
                text = it.text,
                sentAt = it.sentAt.toInstant().toString(),
            )
        }
}
