package com.evmap.serverapp.features.event.application.query

import com.evmap.serverapp.features.event.api.dto.ViewEventReaction
import com.evmap.serverapp.features.event.infra.read.EventReadRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class GetReactionsByEventId(
    private val readRepo: EventReadRepository,
) {
    fun handle(eventId: Long, pageable: Pageable): Page<ViewEventReaction> =
        readRepo.findReactionsByEventId(eventId, pageable)
}
