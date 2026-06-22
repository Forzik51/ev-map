package com.evmap.mobileapp.core.data.dto

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
    val phone: String,
    val dateOfBirth: String,
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
