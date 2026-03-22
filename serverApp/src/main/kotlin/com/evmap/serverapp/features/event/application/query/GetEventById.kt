package com.evmap.serverapp.features.event.application.query

import com.evmap.serverapp.features.event.api.dto.ViewEvent
import com.evmap.serverapp.features.event.infra.read.EventReadRepository
import org.springframework.stereotype.Service

@Service
class GetEventById(private val readRepo: EventReadRepository) {
    fun handle(id: Long): ViewEvent = readRepo.findViewById(id)
}