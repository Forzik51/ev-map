package com.evmap.serverapp.features.auth.api.dto

import com.evmap.serverapp.features.user.api.dto.CreateUser
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank
    val usernameOrEmail: String,

    @field:NotBlank
    val password: String,
)

data class RefreshTokenRequest(
    @field:NotBlank
    val refreshToken: String,
)

data class RegisterRequest(
    @field:Valid
    val user: CreateUser,
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val userId: Long,
    val username: String,
)
