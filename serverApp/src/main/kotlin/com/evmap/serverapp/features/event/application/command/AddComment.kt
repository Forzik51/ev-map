package com.evmap.serverapp.features.event.application.command

import com.evmap.serverapp.features.event.domain.EventRepositoryPort
import org.springframework.stereotype.Service

@Service
class AddComment(
    private val repo: EventRepositoryPort,
) {
    fun handle(eventId: Long, userId: Long, comment: String) {
        repo.addComment(eventId, userId, comment)
    }
}
