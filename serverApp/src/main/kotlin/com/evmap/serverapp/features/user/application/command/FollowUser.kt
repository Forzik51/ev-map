package com.evmap.serverapp.features.user.application.command

import com.evmap.serverapp.features.user.domian.UserRepositoryPort
import org.springframework.stereotype.Service

@Service
class FollowUser(
    private val repo: UserRepositoryPort,
) {
    fun handle(userId: Long, targetUserId: Long) {
        repo.follow(userId, targetUserId)
    }
}
