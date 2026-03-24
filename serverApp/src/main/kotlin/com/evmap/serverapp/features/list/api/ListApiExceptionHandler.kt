package com.evmap.serverapp.features.list.api

import com.evmap.serverapp.features.list.api.controller.ListController
import com.evmap.serverapp.features.list.application.ListNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [ListController::class])
class ListApiExceptionHandler {
    @ExceptionHandler(ListNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: ListNotFoundException): Map<String, String> =
        mapOf("message" to (ex.message ?: "List not found"))

    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleDataIntegrity(): Map<String, String> = mapOf("message" to "Invalid list payload")
}
