package com.evmap.serverapp.features.notification.api.controller

import com.evmap.serverapp.features.notification.api.dto.SendEmailRequest
import com.evmap.serverapp.features.notification.api.dto.EmailDeliverySummary
import com.evmap.serverapp.features.notification.application.command.SendEmailToAllUsers
import com.evmap.serverapp.features.notification.application.command.SendEmailToUserById
import com.evmap.serverapp.shared.security.CurrentUserProvider
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
    private val currentUserProvider: CurrentUserProvider,
) {
    @PostMapping("/all")
    fun sendToAll(@Valid @RequestBody dto: SendEmailRequest): EmailDeliverySummary {
        currentUserProvider.requireAdmin()
        return sendEmailToAllUsers.handle(dto)
    }

    @PostMapping("/user/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun sendToUserById(@PathVariable @Positive userId: Long, @Valid @RequestBody dto: SendEmailRequest) {
        currentUserProvider.requireAdmin()
        sendEmailToUserById.handle(userId, dto)
    }
}
