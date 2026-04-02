package com.evmap.serverapp.features.event.api

import com.evmap.serverapp.features.event.api.controller.EventController
import com.evmap.serverapp.features.event.api.controller.EventPhotoController
import com.evmap.serverapp.features.event.application.EventNotFoundException
import com.evmap.serverapp.features.event.application.EventPhotoNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [EventController::class, EventPhotoController::class])
class EventApiExceptionHandler {
    @ExceptionHandler(EventNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleEventNotFound(ex: EventNotFoundException): Map<String, String> =
        mapOf("message" to (ex.message ?: "Event not found"))

    @ExceptionHandler(EventPhotoNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleEventPhotoNotFound(ex: EventPhotoNotFoundException): Map<String, String> =
        mapOf("message" to (ex.message ?: "Event photo not found"))

    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException): Map<String, String> =
        mapOf("message" to (ex.message ?: "Invalid request"))
}
