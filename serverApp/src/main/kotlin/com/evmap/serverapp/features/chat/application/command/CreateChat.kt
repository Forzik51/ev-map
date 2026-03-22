package com.evmap.serverapp.features.user.application.command

import com.evmap.serverapp.features.user.domian.Chat
import com.evmap.serverapp.features.user.domian.ChatRepositoryPort
import org.springframework.stereotype.Service
import java.time.Instant


@Service
class CreateChat(private val repo: ChatRepositoryPort) {
    fun handle(dto: CreateChat): Long =
        repo.save(
            Chat(

                name = dto.name,
                description = dto.description,
                startsAt = Instant.parse(dto.startsAt),
                locationId = dto.locationId
            )
        )
}