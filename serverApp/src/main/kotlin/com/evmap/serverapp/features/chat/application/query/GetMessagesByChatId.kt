package com.evmap.serverapp.features.chat.application.query

import com.evmap.serverapp.features.chat.api.dto.ViewMessage
import com.evmap.serverapp.features.chat.infra.read.ChatReadRepository
import org.springframework.stereotype.Service

@Service
class GetMessagesByChatId(
    private val readRepo: ChatReadRepository,
) {
    fun handle(chatId: Long, userId: Long, beforeId: Long?, limit: Int?): List<ViewMessage> =
        readRepo.findMessages(
            chatId = chatId,
            userId = userId,
            beforeId = beforeId,
            limit = (limit ?: 50).coerceIn(1, 100),
        )
}
