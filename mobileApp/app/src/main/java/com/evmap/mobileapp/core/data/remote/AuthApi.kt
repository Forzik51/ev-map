package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.AuthResponse
import com.evmap.mobileapp.core.data.dto.LoginRequest
import com.evmap.mobileapp.core.data.dto.RefreshTokenRequest
import com.evmap.mobileapp.core.data.dto.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): AuthResponse

    @POST("api/auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequest): Response<Unit>
}
