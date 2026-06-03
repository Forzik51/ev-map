package com.evmap.serverapp.features.auth.api

import com.evmap.serverapp.features.auth.api.controller.AuthController
import com.evmap.serverapp.features.auth.application.InvalidCredentialsException
import com.evmap.serverapp.features.auth.application.InvalidRefreshTokenException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [AuthController::class])
class AuthApiExceptionHandler {
    @ExceptionHandler(InvalidCredentialsException::class, InvalidRefreshTokenException::class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    fun handleUnauthorized(ex: RuntimeException): Map<String, String> =
        mapOf("message" to (ex.message ?: "Unauthorized"))
}
