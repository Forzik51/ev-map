package com.evmap.serverapp.features.notification.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SendEmailRequest(
    @field:NotBlank
    @field:Size(max = 150)
    val subject: String,

    @field:NotBlank
    @field:Size(max = 10000)
    val body: String,
)
