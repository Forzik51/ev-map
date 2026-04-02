package com.evmap.serverapp.features.user.domian

interface UserRepositoryPort {
    fun save(user: User): Long
    fun delete(userId: Long)
    fun updateDescription(userId: Long, description: String)
    fun updatePath(userId: Long, path: String?)
    fun findPathByUserId(userId: Long): String?
    fun follow(userId: Long, targetUserId: Long)
    fun unfollow(userId: Long, targetUserId: Long)
}
