package com.evmap.serverapp.features.list.application.command

import com.evmap.serverapp.features.list.domian.ListRepositoryPort
import org.springframework.stereotype.Service

@Service
class DeleteList(
    private val repo: ListRepositoryPort,
) {
    fun handle(listId: Long) {
        repo.delete(listId)
    }
}
