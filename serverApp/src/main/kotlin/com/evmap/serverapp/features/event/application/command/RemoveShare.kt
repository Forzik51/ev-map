package com.evmap.serverapp.features.event.application.command

import com.evmap.serverapp.features.event.domain.EventRepositoryPort
import org.springframework.stereotype.Service

@Service
class RemoveShare(
    private val repo: EventRepositoryPort,
) {
    fun handle(eventId: Long, userId: Long) {
        repo.removeShare(eventId, userId)
    }
}
