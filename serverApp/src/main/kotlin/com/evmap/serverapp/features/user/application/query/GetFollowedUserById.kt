package com.evmap.serverapp.features.user.application.query

import com.evmap.serverapp.features.user.api.dto.ViewUser
import com.evmap.serverapp.features.user.infra.read.UserReadRepository
import org.springframework.stereotype.Service

@Service
class GetFollowedUserById(
    private val readRepo: UserReadRepository,
) {
    fun handle(userId: Long): List<ViewUser> = readRepo.findFollowers(userId)
}
