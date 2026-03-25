package com.evmap.serverapp.features.user.application.query

import com.evmap.serverapp.features.user.api.dto.ViewUser
import com.evmap.serverapp.features.user.infra.read.UserReadRepository
import org.springframework.stereotype.Service

@Service
class GetAllUsers(
    private val readRepo: UserReadRepository,
) {
    fun handle(): List<ViewUser> = readRepo.findAll()
}
