package com.evmap.mobileapp.feature.profile.data

import com.evmap.mobileapp.core.data.dto.UpdateProfileRequest
import com.evmap.mobileapp.core.data.dto.UserDto
import com.evmap.mobileapp.core.data.dto.UserShortDto
import com.evmap.mobileapp.core.data.remote.UsersApi
import com.evmap.mobileapp.core.data.session.TokenStore
import kotlinx.coroutines.CancellationException
import okhttp3.MultipartBody
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsersRepository @Inject constructor(
    private val usersApi: UsersApi,
    private val tokenStore: TokenStore,
) {
    fun currentUserId(): Long? = tokenStore.session()?.userId?.takeIf { it > 0 }

    suspend fun getMe(): Result<UserDto> = call {
        usersApi.getMe().also { tokenStore.updateUsername(it.id, it.username) }
    }

    suspend fun getUser(userId: Long): Result<UserDto> = call {
        usersApi.getUser(userId)
    }

    suspend fun getFollowers(userId: Long): Result<List<UserShortDto>> = call {
        usersApi.getFollowers(userId)
    }

    suspend fun getFollowing(userId: Long): Result<List<UserShortDto>> = call {
        usersApi.getFollowing(userId)
    }

    suspend fun updateProfile(profile: UpdateProfileRequest): Result<UserDto> = call {
        usersApi.updateMe(profile).also { tokenStore.updateUsername(it.id, it.username) }
    }

    suspend fun replacePhoto(userId: Long, file: MultipartBody.Part) = call { usersApi.replacePhoto(userId, file) }

    suspend fun deletePhoto(userId: Long) = call { usersApi.deletePhoto(userId).requireSuccess() }

    suspend fun follow(targetUserId: Long): Result<Unit> = call {
        val userId = requireNotNull(currentUserId()) { "Missing current user" }
        usersApi.follow(userId, targetUserId).requireSuccess()
    }

    suspend fun unfollow(targetUserId: Long): Result<Unit> = call {
        val userId = requireNotNull(currentUserId()) { "Missing current user" }
        usersApi.unfollow(userId, targetUserId).requireSuccess()
    }

    private fun Response<Unit>.requireSuccess() {
        if (!isSuccessful) throw HttpException(this)
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
