package com.evmap.serverapp.features.user.application.query

import com.evmap.serverapp.features.user.api.dto.ViewUserShort
import com.evmap.serverapp.features.user.infra.read.UserReadRepository
import org.springframework.stereotype.Service

@Service
class GetFollowingUserById(
    private val readRepo: UserReadRepository,
) {
    fun handle(userId: Long): List<ViewUserShort> = readRepo.findFollowing(userId)
}
