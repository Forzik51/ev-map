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
    @field:com.evmap.serverapp.shared.security.ValidOptionalPhone
    val phone: String?,
    @field:Size(max = 150)
    val pageDescription: String,
    @field:jakarta.validation.constraints.PastOrPresent val dateOfBirth: java.time.LocalDate? = null,
    val updateDateOfBirth: Boolean = false,
)

internal const val PROFILE_PHONE_PATTERN = "^$|^[+0-9 ()-]{7,15}$"
