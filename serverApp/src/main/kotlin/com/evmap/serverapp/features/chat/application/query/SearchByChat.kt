package com.evmap.serverapp.features.chat.application.query

import com.evmap.serverapp.features.chat.infra.read.ChatReadRepository
import org.springframework.stereotype.Service

@Service
class SearchByChat(
    private val readRepo: ChatReadRepository,
) {
    fun handle(chatId: Long, query: String): List<String> = readRepo.searchByChat(chatId, query)
}
