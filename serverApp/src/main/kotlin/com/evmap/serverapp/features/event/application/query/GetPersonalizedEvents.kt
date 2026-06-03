package com.evmap.serverapp.features.event.application.query

import com.evmap.serverapp.features.event.api.dto.ViewEvent
import com.evmap.serverapp.features.event.infra.read.EventReadRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class GetPersonalizedEvents(
    private val readRepo: EventReadRepository,
) {
    fun handle(userId: Long, lat: Double?, lng: Double?, pageable: Pageable): Page<ViewEvent> =
        readRepo.findPersonalizedViews(userId, lat, lng, pageable)
}
