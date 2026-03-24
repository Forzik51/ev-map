package com.evmap.serverapp.features.chat.application.query

import com.evmap.serverapp.features.chat.infra.read.ChatReadRepository
import org.springframework.stereotype.Service

@Service
class GetMessagesByChatId(
    private val readRepo: ChatReadRepository,
) {
    fun handle(chatId: Long): List<String> = readRepo.findMessages(chatId)
}
