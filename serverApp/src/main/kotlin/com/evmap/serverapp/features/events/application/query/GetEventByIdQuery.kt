package com.evmap.serverapp.features.events.application.query

import com.evmap.serverapp.features.events.api.dto.EventView
import com.evmap.serverapp.features.events.infra.read.EventReadRepository
import org.springframework.stereotype.Service

@Service
class GetEventByIdQuery(private val readRepo: EventReadRepository) {
    fun handle(id: Long): EventView = readRepo.findViewById(id)
}