package com.evmap.serverapp.features.event.application.command

import com.evmap.serverapp.features.event.api.dto.CreateEvent
import com.evmap.serverapp.features.event.domain.Event
import com.evmap.serverapp.features.event.domain.EventRepositoryPort
import org.springframework.stereotype.Service
import com.evmap.serverapp.features.event.application.toEvent

@Service
class UpdateEvent(
    private val repo: EventRepositoryPort,
) {
    fun handle(eventId: Long, dto: CreateEvent, userId: Long) {
        repo.update(
            eventId,
            dto.toEvent(userId,eventId)
        )
    }
}
