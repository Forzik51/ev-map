package com.evmap.serverapp.features.event.application.command

import com.evmap.serverapp.features.event.api.dto.CreateEvent
import com.evmap.serverapp.features.event.domain.Event
import com.evmap.serverapp.features.event.domain.EventRepositoryPort
import org.springframework.stereotype.Service

@Service
class CreateEvent(private val repo: EventRepositoryPort) {
    fun handle(dto: CreateEvent): Long =
        repo.save(
            Event(
                name = dto.name,
                description = dto.description,
                startsAt = dto.startsAt,
                locationId = dto.locationId
            )
        )
}
