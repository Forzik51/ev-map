package com.evmap.serverapp.features.chat.application.command

import com.evmap.serverapp.features.chat.api.dto.ViewMessage
import com.evmap.serverapp.features.chat.domian.ChatAttachment
import com.evmap.serverapp.features.chat.domian.ChatRepositoryPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SendMessage(
    private val repo: ChatRepositoryPort,
    private val publisher: ApplicationEventPublisher,
) {
    @Transactional
    fun handle(chatId: Long, userId: Long, message: String, clientMessageId: UUID? = null, attachment: ChatAttachment? = null): ViewMessage =
        repo.sendMessage(chatId, userId, message, clientMessageId, attachment).let {
            ViewMessage(
                id = it.id,
                chatId = it.chatId,
                senderId = it.senderId,
                senderUsername = it.senderUsername,
                text = it.text,
                sentAt = it.sentAt.toInstant().toString(),
                attachment = it.attachment,
            ).also { view -> if (it.created) publisher.publishEvent(view) }
        }
}
