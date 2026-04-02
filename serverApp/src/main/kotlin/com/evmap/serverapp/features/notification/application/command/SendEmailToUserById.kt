package com.evmap.serverapp.features.notification.application.command

import com.evmap.serverapp.features.notification.api.dto.SendEmailRequest
import com.evmap.serverapp.features.user.infra.read.UserReadRepository
import com.evmap.serverapp.shared.email.application.EmailSenderPort
import com.evmap.serverapp.shared.email.domain.EmailMessage
import org.springframework.stereotype.Service

@Service
class SendEmailToUserById(
    private val userReadRepository: UserReadRepository,
    private val emailSender: EmailSenderPort,
) {
    fun handle(userId: Long, dto: SendEmailRequest) {
        val email = userReadRepository.findEmailByUserId(userId)
        emailSender.send(
            EmailMessage(
                to = email,
                subject = dto.subject,
                body = dto.body,
            )
        )
    }
}
