package com.evmap.mobileapp.core.data.dto

data class ForgotPasswordRequest(val email: String)
data class ForgotPasswordResponse(val requestId: String, val resendAfterSeconds: Long, val message: String)
data class VerifyPasswordCodeRequest(val requestId: String, val code: String)
data class VerifyPasswordCodeResponse(val resetToken: String, val expiresInSeconds: Long)
data class ResetPasswordRequest(val resetToken: String, val newPassword: String)

data class LoginRequest(
    val usernameOrEmail: String,
    val password: String,
)

data class RefreshTokenRequest(
    val refreshToken: String,
)

data class RegisterRequest(
    val user: CreateUserRequest,
)

data class CreateUserRequest(
    val name: String,
    val surname: String?,
    val email: String,
    val phone: String?,
    val dateOfBirth: String?,
    val username: String,
    val pageDescription: String,
    val password: String,
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val userId: Long,
    val username: String,
)
