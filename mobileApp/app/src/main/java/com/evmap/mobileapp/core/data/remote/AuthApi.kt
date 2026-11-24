package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.AuthResponse
import com.evmap.mobileapp.core.data.dto.LoginRequest
import com.evmap.mobileapp.core.data.dto.RefreshTokenRequest
import com.evmap.mobileapp.core.data.dto.RegisterRequest
import com.evmap.mobileapp.core.data.dto.ForgotPasswordRequest
import com.evmap.mobileapp.core.data.dto.ForgotPasswordResponse
import com.evmap.mobileapp.core.data.dto.VerifyPasswordCodeRequest
import com.evmap.mobileapp.core.data.dto.VerifyPasswordCodeResponse
import com.evmap.mobileapp.core.data.dto.ResetPasswordRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/password/forgot")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): ForgotPasswordResponse

    @POST("api/auth/password/verify")
    suspend fun verifyPasswordCode(@Body request: VerifyPasswordCodeRequest): VerifyPasswordCodeResponse

    @POST("api/auth/password/reset")
    suspend fun resetPassword(@Body request: ResetPasswordRequest)

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("api/auth/refresh")
    suspend fun refresh(@Body request: RefreshTokenRequest): AuthResponse

    @POST("api/auth/logout")
    suspend fun logout(@Body request: RefreshTokenRequest): Response<Unit>
}
