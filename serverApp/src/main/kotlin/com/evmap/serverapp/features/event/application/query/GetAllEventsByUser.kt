package com.evmap.serverapp.features.event.application.query

import com.evmap.serverapp.features.event.api.dto.ViewEvent
import com.evmap.serverapp.features.event.infra.read.EventReadRepository
import org.springframework.stereotype.Service
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Service
class GetAllEventsByUser(
    private val readRepo: EventReadRepository
) {
    //fun handle(): List<EventView> = readRepo.findAllViews()

    fun handle(pageable: Pageable): Page<ViewEvent> =
        readRepo.findAllViews(pageable)
}