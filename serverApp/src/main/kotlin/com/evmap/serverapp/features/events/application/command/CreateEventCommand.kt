package com.evmap.serverapp.features.events.application.command

import com.evmap.serverapp.features.events.api.dto.CreateEventDto
import com.evmap.serverapp.features.events.domain.Event
import com.evmap.serverapp.features.events.domain.EventRepositoryPort
import org.springframework.stereotype.Service

@Service
class CreateEventCommand(private val repo: EventRepositoryPort) {
    fun handle(dto: CreateEventDto): Long =
        repo.save(
            Event(
                name = dto.name,
                description = dto.description,
                startsAt = java.time.Instant.parse(dto.startsAt),
                locationId = dto.locationId
            )
        )
}