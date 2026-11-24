package com.evmap.serverapp.features.user.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class UpdateProfile(
    @field:NotBlank @field:Size(max = 20)
    val name: String,
    @field:Size(max = 20)
    val surname: String?,
    @field:NotBlank @field:Size(max = 20)
    val username: String,
    @field:Pattern(regexp = PROFILE_PHONE_PATTERN, message = "Enter a phone number with 7 to 15 characters")
    val phone: String,
    @field:Size(max = 150)
    val pageDescription: String,
)

internal const val PROFILE_PHONE_PATTERN = "^[+0-9 ()-]{7,15}$"
