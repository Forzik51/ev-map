package com.evmap.serverapp.features.list.application.command

import com.evmap.serverapp.features.list.domian.ListRepositoryPort
import org.springframework.stereotype.Service

@Service
class ChangeListName(
    private val repo: ListRepositoryPort,
) {
    fun handle(listId: Long, name: String) {
        repo.changeName(listId, name)
    }
}
