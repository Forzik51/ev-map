package com.evmap.serverapp.features.user.application.command

import com.evmap.serverapp.features.user.api.dto.UpdateProfile
import com.evmap.serverapp.features.user.api.dto.ViewUser
import com.evmap.serverapp.features.user.application.query.GetUserById
import com.evmap.serverapp.features.user.domian.UserRepositoryPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EditUserInfo(
    private val repo: UserRepositoryPort,
    private val getUserById: GetUserById,
) {
    fun handle(userId: Long, description: String) {
        repo.updateDescription(userId, description)
    }

    @Transactional
    fun handle(userId: Long, profile: UpdateProfile): ViewUser {
        repo.updateProfile(userId, profile.copy(
            name = profile.name.trim(),
            surname = profile.surname?.trim()?.takeIf { it.isNotEmpty() },
            username = profile.username.trim(),
        ))
        return getUserById.handle(userId)
    }
}
