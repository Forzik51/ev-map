package com.evmap.serverapp.features.event.application.command

import com.evmap.serverapp.features.event.domain.EventRepositoryPort
import org.springframework.stereotype.Service

@Service
class AddReaction(
    private val repo: EventRepositoryPort,
) {
    fun handle(eventId: Long, userId: Long, reaction: String) {
        repo.addReaction(eventId, userId, reaction)
    }
}
