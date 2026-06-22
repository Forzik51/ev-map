package com.evmap.mobileapp.feature.auth.data

import com.evmap.mobileapp.core.data.dto.AuthResponse
import com.evmap.mobileapp.core.data.dto.CreateUserRequest
import com.evmap.mobileapp.core.data.dto.LoginRequest
import com.evmap.mobileapp.core.data.dto.RefreshTokenRequest
import com.evmap.mobileapp.core.data.dto.RegisterRequest
import com.evmap.mobileapp.core.data.remote.AuthApi
import com.evmap.mobileapp.core.data.session.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
) {
    fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    suspend fun login(usernameOrEmail: String, password: String): Result<AuthResponse> =
        runCatching {
            authApi.login(LoginRequest(usernameOrEmail, password))
        }.onSuccess(tokenStore::save)

    suspend fun register(user: CreateUserRequest): Result<AuthResponse> =
        runCatching {
            authApi.register(RegisterRequest(user))
        }.onSuccess(tokenStore::save)

    suspend fun logout(): Result<Unit> =
        runCatching {
            val refreshToken = tokenStore.refreshToken()
            if (!refreshToken.isNullOrBlank()) {
                authApi.logout(RefreshTokenRequest(refreshToken))
            }
            tokenStore.clear()
        }
}
