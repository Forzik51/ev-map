package com.evmap.serverapp.features.user.api

import com.evmap.serverapp.features.user.api.controller.UserController
import com.evmap.serverapp.features.user.api.controller.UserPhotoController
import com.evmap.serverapp.features.user.application.UserNotFoundException
import com.evmap.serverapp.features.user.application.UserPhotoNotFoundException
import com.evmap.serverapp.features.user.application.UsernameUnavailableException
import org.jooq.exception.IntegrityConstraintViolationException
import org.postgresql.util.PSQLException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(assignableTypes = [UserController::class, UserPhotoController::class])
class UserApiExceptionHandler {
    @ExceptionHandler(UserNotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: UserNotFoundException): Map<String, String> =
        mapOf("message" to (ex.message ?: "User not found"))

    @ExceptionHandler(DataIntegrityViolationException::class, IntegrityConstraintViolationException::class)
    fun handleDataIntegrity(ex: RuntimeException): ResponseEntity<Map<String, Any>> {
        val constraint = generateSequence<Throwable>(ex) { it.cause }
            .filterIsInstance<PSQLException>()
            .firstOrNull { it.sqlState == "23505" }?.serverErrorMessage?.constraint
        val field = when (constraint) {
            "uq_app_user_username_lower" -> "username" to "This username is already taken"
            "v3_app_user_phone_uk" -> "phone" to "This phone number is already in use"
            else -> null
        }
        return if (field != null) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("message" to field.second, "fieldErrors" to mapOf(field)))
        } else {
            ResponseEntity.badRequest().body(mapOf("message" to "Invalid user payload"))
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidation(ex: MethodArgumentNotValidException): Map<String, Any> = mapOf(
        "message" to "Check the entered data",
        "fieldErrors" to ex.bindingResult.fieldErrors.groupBy { it.field }.mapValues { (_, errors) ->
            errors.first().defaultMessage ?: "Invalid value"
        },
    )

    @ExceptionHandler(UsernameUnavailableException::class)
    @ResponseStatus(HttpStatus.CONFLICT)
    fun handleUsernameUnavailable(ex: UsernameUnavailableException): Map<String, Any> =
        mapOf("message" to ex.message.orEmpty(), "fieldErrors" to mapOf("username" to ex.message.orEmpty()))

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
