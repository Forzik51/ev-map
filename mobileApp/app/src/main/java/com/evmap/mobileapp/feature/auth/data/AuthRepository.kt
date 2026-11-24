package com.evmap.mobileapp.feature.auth.data

import android.util.Log
import com.evmap.mobileapp.core.data.dto.AuthResponse
import com.evmap.mobileapp.core.data.dto.CreateUserRequest
import com.evmap.mobileapp.core.data.dto.LoginRequest
import com.evmap.mobileapp.core.data.dto.RefreshTokenRequest
import com.evmap.mobileapp.core.data.dto.RegisterRequest
import com.evmap.mobileapp.core.data.remote.AuthApi
import com.evmap.mobileapp.core.data.session.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    @Named("refreshAuthApi") private val authApi: AuthApi,
    private val tokenStore: TokenStore,
) {
    fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    suspend fun login(usernameOrEmail: String, password: String): Result<AuthResponse> =
        runCatching {
            authApi.login(LoginRequest(usernameOrEmail, password)).also(tokenStore::save)
        }

    suspend fun register(user: CreateUserRequest): Result<AuthResponse> =
        runCatching {
            authApi.register(RegisterRequest(user)).also(tokenStore::save)
        }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO + NonCancellable) {
        val active = tokenStore.takeAndClear() ?: return@withContext Result.success(Unit)
        runCatching {
            val response = authApi.logout(RefreshTokenRequest(active.refreshToken))
            if (!response.isSuccessful) throw HttpException(response)
        }.onFailure { Log.w("AuthRepository", "The server could not confirm logout") }
    }
}
