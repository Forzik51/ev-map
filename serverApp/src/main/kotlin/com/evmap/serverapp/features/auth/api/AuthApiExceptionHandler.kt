package com.evmap.serverapp.features.auth.api

import com.evmap.serverapp.features.auth.api.controller.AuthController
import com.evmap.serverapp.features.auth.application.InvalidCredentialsException
import com.evmap.serverapp.features.auth.application.InvalidRefreshTokenException
import com.evmap.serverapp.features.auth.application.InvalidPasswordResetCode
import com.evmap.serverapp.features.auth.application.InvalidPasswordResetToken
import com.evmap.serverapp.features.auth.application.PasswordResetRateLimited
import org.springframework.http.ResponseEntity
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [AuthController::class])
class AuthApiExceptionHandler {
    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleConflict(): Map<String, String> = mapOf("message" to "Account details are already in use")

    @ExceptionHandler(InvalidPasswordResetCode::class, InvalidPasswordResetToken::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleReset(ex: RuntimeException): Map<String, String> = mapOf(
        "code" to if (ex is InvalidPasswordResetCode) "INVALID_OR_EXPIRED_CODE" else "INVALID_OR_EXPIRED_RESET_TOKEN",
        "message" to ex.message.orEmpty(),
    )

    @ExceptionHandler(PasswordResetRateLimited::class)
    fun handleRateLimit(ex: PasswordResetRateLimited): ResponseEntity<Map<String, Any>> =
        ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header("Retry-After", ex.retryAfterSeconds.toString())
            .body(mapOf("code" to "RESET_RATE_LIMITED", "message" to ex.message.orEmpty(), "retryAfterSeconds" to ex.retryAfterSeconds))

    @ExceptionHandler(InvalidCredentialsException::class, InvalidRefreshTokenException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleUnauthorized(ex: RuntimeException): Map<String, String> =
        mapOf("message" to (ex.message ?: "Unauthorized"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: MethodArgumentNotValidException): Map<String, Any> {
        val errors = ex.bindingResult.fieldErrors.associate { error ->
            error.field to (error.defaultMessage ?: "Invalid value")
        }
        return mapOf(
            "message" to errors.entries.joinToString("; ") { (field, message) -> "$field: $message" },
            "errors" to errors,
        )
    }
}
