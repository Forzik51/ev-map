package com.evmap.serverapp.features.list.application.query

import com.evmap.serverapp.features.list.api.dto.ViewList
import com.evmap.serverapp.features.list.infra.read.ListReadRepository
import org.springframework.stereotype.Service

@Service
class GetAllListsByUser(
    private val readRepo: ListReadRepository,
) {
    fun handle(userId: Long): List<ViewList> = readRepo.findAllByUserId(userId)
}
