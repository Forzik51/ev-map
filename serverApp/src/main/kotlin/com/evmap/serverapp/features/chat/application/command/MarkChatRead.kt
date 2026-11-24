package com.evmap.serverapp.features.chat.application.command

import com.evmap.serverapp.features.chat.api.dto.ViewReadEvent
import com.evmap.serverapp.features.chat.domian.ChatRepositoryPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MarkChatRead(
    private val repo: ChatRepositoryPort,
    private val publisher: ApplicationEventPublisher,
) {
    @Transactional
    fun handle(chatId: Long, userId: Long, lastReadMessageId: Long) {
        val sentAt = repo.markRead(chatId, userId, lastReadMessageId) ?: return
        publisher.publishEvent(ViewReadEvent(chatId, userId, lastReadMessageId, sentAt.toInstant().toString()))
    }
}
