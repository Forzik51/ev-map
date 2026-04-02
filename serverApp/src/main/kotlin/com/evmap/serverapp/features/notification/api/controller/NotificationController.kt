package com.evmap.serverapp.features.notification.api.controller

import com.evmap.serverapp.features.notification.api.dto.SendEmailRequest
import com.evmap.serverapp.features.notification.application.command.SendEmailToAllUsers
import com.evmap.serverapp.features.notification.application.command.SendEmailToUserById
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
@RequestMapping("/api/notifications/email")
class NotificationController(
    private val sendEmailToAllUsers: SendEmailToAllUsers,
    private val sendEmailToUserById: SendEmailToUserById,
) {
    @PostMapping("/all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun sendToAll(@Valid @RequestBody dto: SendEmailRequest) {
        sendEmailToAllUsers.handle(dto)
    }

    @PostMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun sendToUserById(@PathVariable @Positive userId: Long, @Valid @RequestBody dto: SendEmailRequest) {
        sendEmailToUserById.handle(userId, dto)
    }
}
