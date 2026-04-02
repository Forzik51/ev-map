package com.evmap.serverapp.features.user.api

import com.evmap.serverapp.features.user.api.controller.UserController
import com.evmap.serverapp.features.user.api.controller.UserPhotoController
import com.evmap.serverapp.features.user.application.UserNotFoundException
import com.evmap.serverapp.features.user.application.UserPhotoNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [UserController::class, UserPhotoController::class])
class UserApiExceptionHandler {
    @ExceptionHandler(UserNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: UserNotFoundException): Map<String, String> =
        mapOf("message" to (ex.message ?: "User not found"))

    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleDataIntegrity(): Map<String, String> = mapOf("message" to "Invalid user payload")

    @ExceptionHandler(UserPhotoNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handlePhotoNotFound(ex: UserPhotoNotFoundException): Map<String, String> =
        mapOf("message" to (ex.message ?: "User photo not found"))

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException): Map<String, String> =
        mapOf("message" to (ex.message ?: "Invalid request"))

    @ExceptionHandler(IllegalStateException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleIllegalState(ex: IllegalStateException): Map<String, String> =
        mapOf("message" to (ex.message ?: "Request conflict"))
}
