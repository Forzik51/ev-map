package com.evmap.serverapp.features.notification.api

import com.evmap.serverapp.features.notification.api.controller.NotificationController
import com.evmap.serverapp.features.user.application.UserNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [NotificationController::class])
class NotificationApiExceptionHandler {
    @ExceptionHandler(UserNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: UserNotFoundException): Map<String, String> =
        mapOf("message" to (ex.message ?: "User not found"))
}
