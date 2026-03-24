package com.evmap.serverapp.features.list.application.query

import com.evmap.serverapp.features.list.infra.read.ListReadRepository
import org.springframework.stereotype.Service

@Service
class GetEventsFromList(
    private val readRepo: ListReadRepository,
) {
    fun handle(listId: Long): List<Long> = readRepo.findEventIds(listId)
}
