package com.evmap.serverapp.features.chat.application.query

import com.evmap.serverapp.features.chat.api.dto.ViewChat
import com.evmap.serverapp.features.chat.infra.read.ChatReadRepository
import org.springframework.stereotype.Service

@Service
class GetChatInfo(
    private val readRepo: ChatReadRepository,
) {
    fun handle(chatId: Long): ViewChat = readRepo.findById(chatId)
}
