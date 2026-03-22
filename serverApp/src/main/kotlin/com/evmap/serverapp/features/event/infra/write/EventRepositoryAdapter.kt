package com.evmap.serverapp.features.event.infra.write

import com.evmap.serverapp.features.event.domain.Event
import com.evmap.serverapp.features.event.domain.EventRepositoryPort
import org.springframework.stereotype.Repository

@Repository
class EventRepositoryAdapter(
    private val jpa: SpringDataEventJpa
) : EventRepositoryPort {
    override fun save(event: Event): Long {
        val e = jpa.save(
            EventEntity(
                name = event.name,
                description = event.description,
                startsAt = event.startsAt,
                locationId = event.locationId
            )
        )
        return e.id!!
    }
}