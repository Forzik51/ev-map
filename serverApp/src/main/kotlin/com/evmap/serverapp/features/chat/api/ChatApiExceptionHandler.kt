package com.evmap.serverapp.features.chat.api

import com.evmap.serverapp.features.chat.api.controller.ChatController
import com.evmap.serverapp.features.chat.application.ChatNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [ChatController::class])
class ChatApiExceptionHandler {
    @ExceptionHandler(ChatNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: ChatNotFoundException): Map<String, String> =
        mapOf("message" to (ex.message ?: "Chat not found"))

    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleDataIntegrity(): Map<String, String> = mapOf("message" to "Invalid chat payload")
}
