package com.evmap.serverapp.features.event.application.command

import com.evmap.serverapp.features.event.api.dto.CreateEvent
import com.evmap.serverapp.features.event.domain.Event
import com.evmap.serverapp.features.event.domain.EventRepositoryPort
import org.springframework.stereotype.Service

@Service
class UpdateEvent(
    private val repo: EventRepositoryPort,
) {
    fun handle(eventId: Long, dto: CreateEvent) {
        repo.update(
            eventId,
            Event(
                id = eventId,
                name = dto.name,
                description = dto.description,
                startsAt = dto.startsAt,
                endsAt = dto.endsAt,
                locationId = dto.locationId,
                userId = dto.userId,
            )
        )
    }
}
