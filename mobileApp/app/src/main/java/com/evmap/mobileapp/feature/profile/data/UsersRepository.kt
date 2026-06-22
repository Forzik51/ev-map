package com.evmap.mobileapp.feature.profile.data

import com.evmap.mobileapp.core.data.dto.EventDto
import com.evmap.mobileapp.core.data.dto.UserDto
import com.evmap.mobileapp.core.data.dto.UserShortDto
import com.evmap.mobileapp.core.data.remote.EventsApi
import com.evmap.mobileapp.core.data.remote.UsersApi
import com.evmap.mobileapp.core.data.session.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsersRepository @Inject constructor(
    private val usersApi: UsersApi,
    private val eventsApi: EventsApi,
    private val tokenStore: TokenStore,
) {
    fun currentUserId(): Long? = tokenStore.session()?.userId?.takeIf { it > 0 }

    suspend fun getMe(): Result<UserDto> = runCatching {
        usersApi.getMe()
    }

    suspend fun getUser(userId: Long): Result<UserDto> = runCatching {
        usersApi.getUser(userId)
    }

    suspend fun getFollowers(userId: Long): Result<List<UserShortDto>> = runCatching {
        usersApi.getFollowers(userId)
    }

    suspend fun getFollowing(userId: Long): Result<List<UserShortDto>> = runCatching {
        usersApi.getFollowing(userId)
    }

    suspend fun getUserEvents(userId: Long): Result<List<EventDto>> = runCatching {
        eventsApi.getEvents(page = 0, size = 50).content.filter { it.userId == userId }
    }

    suspend fun updateDescription(userId: Long, description: String): Result<Unit> = runCatching {
        usersApi.updateDescription(userId, description)
        Unit
    }

    suspend fun follow(targetUserId: Long): Result<Unit> = runCatching {
        val userId = requireNotNull(currentUserId()) { "Missing current user" }
        usersApi.follow(userId, targetUserId)
        Unit
    }

    suspend fun unfollow(targetUserId: Long): Result<Unit> = runCatching {
        val userId = requireNotNull(currentUserId()) { "Missing current user" }
        usersApi.unfollow(userId, targetUserId)
        Unit
    }
}
