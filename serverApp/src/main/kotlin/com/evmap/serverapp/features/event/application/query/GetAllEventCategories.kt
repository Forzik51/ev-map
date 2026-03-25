package com.evmap.serverapp.features.event.application.query

import com.evmap.serverapp.features.event.api.dto.ViewEventCategory
import com.evmap.serverapp.features.event.infra.read.EventReadRepository
import org.springframework.stereotype.Service

@Service
class GetAllEventCategories(
    private val readRepo: EventReadRepository,
) {
    fun handle(): List<ViewEventCategory> = readRepo.findAllCategories()
}
