package com.evmap.serverapp.features.auth.api.dto

import com.evmap.serverapp.shared.security.ValidPassword
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.util.UUID

data class ForgotPasswordRequest(
    @field:NotBlank @field:Email @field:Size(max = 254) val email: String,
)

data class ForgotPasswordResponse(
    val requestId: UUID,
    val resendAfterSeconds: Long,
    val message: String = "If an account exists for this email, a recovery code will be sent.",
)

data class VerifyPasswordCodeRequest(
    val requestId: UUID,
    @field:Pattern(regexp = "[0-9]{8}") val code: String,
)

data class VerifyPasswordCodeResponse(val resetToken: String, val expiresInSeconds: Long)

data class ResetPasswordRequest(
    @field:Pattern(regexp = "[A-Za-z0-9_-]{43}") val resetToken: String,
    @field:ValidPassword val newPassword: String,
)
