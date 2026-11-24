package com.evmap.serverapp.features.user.api.dto

import com.evmap.serverapp.shared.security.ValidPassword

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class CreateUser(
    @field:NotBlank
    @field:Size(max = 20)
    val name: String,

    @field:Size(max = 20)
    val surname: String?,

    @field:Email
    @field:Size(max = 254)
    val email: String,

    @field:Pattern(regexp = PROFILE_PHONE_PATTERN)
    val phone: String,

    val dateOfBirth: LocalDate,

    @field:NotBlank
    @field:Size(max = 20)
    val username: String,

    @field:NotBlank
    @field:Size(max = 150)
    val pageDescription: String,

    @field:ValidPassword
    val password: String,
)
