package com.evmap.serverapp.features.search.api

import com.evmap.serverapp.features.search.api.controller.SearchController
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [SearchController::class])
class SearchApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleIllegalArgument(ex: IllegalArgumentException): Map<String, String> =
        mapOf("message" to (ex.message ?: "Invalid request"))
}
