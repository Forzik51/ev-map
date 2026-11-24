package com.evmap.serverapp.features.user.domian

import com.evmap.serverapp.features.user.api.dto.UpdateProfile

interface UserRepositoryPort {
    fun save(user: User): Long
    fun delete(userId: Long)
    fun updateDescription(userId: Long, description: String)
    fun updateProfile(userId: Long, profile: UpdateProfile)
    fun updatePath(userId: Long, path: String?)
    fun findPathByUserId(userId: Long): String?
    fun lockUserAndFindPath(userId: Long): String?
    fun follow(userId: Long, targetUserId: Long)
    fun unfollow(userId: Long, targetUserId: Long)
}
