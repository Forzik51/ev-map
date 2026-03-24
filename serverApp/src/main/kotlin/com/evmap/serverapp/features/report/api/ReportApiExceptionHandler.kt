package com.evmap.serverapp.features.report.api

import com.evmap.serverapp.features.report.api.controller.ReportController
import com.evmap.serverapp.features.report.application.ReportNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [ReportController::class])
class ReportApiExceptionHandler {
    @ExceptionHandler(ReportNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: ReportNotFoundException): Map<String, String> =
        mapOf("message" to (ex.message ?: "Report not found"))

    @ExceptionHandler(DataIntegrityViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleDataIntegrity(): Map<String, String> = mapOf("message" to "Invalid report payload")
}
