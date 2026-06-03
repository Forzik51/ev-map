package com.evmap.serverapp.features.chat.application.command

import com.evmap.serverapp.features.chat.api.dto.CreateChat as CreateChatRequest
import com.evmap.serverapp.features.chat.domian.Chat
import com.evmap.serverapp.features.chat.domian.ChatRepositoryPort
import org.springframework.stereotype.Service

@Service
class CreateChat(private val repo: ChatRepositoryPort) {
    fun handle(dto: CreateChatRequest, creatorUserId: Long): Long =
        repo.save(
            chat = Chat(name = dto.name),
            creatorUserId = creatorUserId,
            participantUserIds = dto.participantUserIds
        )
}
