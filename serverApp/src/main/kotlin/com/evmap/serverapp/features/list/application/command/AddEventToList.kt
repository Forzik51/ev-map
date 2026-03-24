package com.evmap.serverapp.features.list.application.command

import com.evmap.serverapp.features.list.domian.ListRepositoryPort
import org.springframework.stereotype.Service

@Service
class AddEventToList(
    private val repo: ListRepositoryPort,
) {
    fun handle(listId: Long, eventId: Long) {
        repo.addEvent(listId, eventId)
    }
}
