package com.evmap.serverapp.shared.api

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.http.converter.HttpMessageNotReadableException

class FormValidationException(val fields: Map<String,String>) : IllegalArgumentException("Перевірте заповнені поля")

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class FormValidationAdvice {
    @ExceptionHandler(FormValidationException::class)
    fun custom(ex: FormValidationException) = ResponseEntity.badRequest().body(mapOf("code" to "VALIDATION_ERROR", "message" to ex.message, "fieldErrors" to ex.fields))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalid(ex: MethodArgumentNotValidException) = ResponseEntity.badRequest().body(mapOf("code" to "VALIDATION_ERROR", "message" to "Перевірте заповнені поля", "fieldErrors" to ex.bindingResult.fieldErrors.associate { it.field.removePrefix("user.") to (it.defaultMessage ?: "Некоректне значення") }))

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun unreadable() = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("code" to "INVALID_REQUEST", "message" to "Перевірте формат дат і заповнення полів"))
}
