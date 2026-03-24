package com.evmap.serverapp.features.list.application.command

import com.evmap.serverapp.features.list.api.dto.CreateList as CreateListRequest
import com.evmap.serverapp.features.list.domian.ListRepositoryPort
import com.evmap.serverapp.features.list.domian.UserList
import org.springframework.stereotype.Service

@Service
class CreateList(private val repo: ListRepositoryPort) {
    fun handle(dto: CreateListRequest): Long =
        repo.save(
            UserList(
                name = dto.name,
                userId = dto.userId,
            )
        )
}
