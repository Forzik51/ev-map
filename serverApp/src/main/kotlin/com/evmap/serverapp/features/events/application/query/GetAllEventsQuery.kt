package com.evmap.serverapp.features.events.application.query

import com.evmap.serverapp.features.events.api.dto.EventView
import com.evmap.serverapp.features.events.infra.read.EventReadRepository
import org.springframework.stereotype.Service
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

@Service
class GetAllEventsQuery(
    private val readRepo: EventReadRepository
) {
    //fun handle(): List<EventView> = readRepo.findAllViews()

    fun handle(pageable: Pageable): Page<EventView> =
        readRepo.findAllViewsV2(pageable)
}