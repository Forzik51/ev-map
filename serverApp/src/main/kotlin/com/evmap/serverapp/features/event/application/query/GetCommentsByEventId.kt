package com.evmap.serverapp.features.event.application.query

import com.evmap.serverapp.features.event.api.dto.ViewEventComment
import com.evmap.serverapp.features.event.infra.read.EventReadRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class GetCommentsByEventId(
    private val readRepo: EventReadRepository,
) {
    fun handle(eventId: Long, pageable: Pageable): Page<ViewEventComment> =
        readRepo.findCommentsByEventId(eventId, pageable)
}
