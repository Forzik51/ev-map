package com.evmap.serverapp.features.user.application.command

import com.evmap.serverapp.features.user.domian.UserRepositoryPort
import org.springframework.stereotype.Service

@Service
class EditUserInfo(
    private val repo: UserRepositoryPort,
) {
    fun handle(userId: Long, description: String) {
        repo.updateDescription(userId, description)
    }
}
