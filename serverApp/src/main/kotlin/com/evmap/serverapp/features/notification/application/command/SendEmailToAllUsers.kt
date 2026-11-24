package com.evmap.serverapp.features.notification.application.command

import com.evmap.serverapp.features.notification.api.dto.SendEmailRequest
import com.evmap.serverapp.features.notification.api.dto.EmailDeliverySummary
import com.evmap.serverapp.features.user.infra.read.UserReadRepository
import com.evmap.serverapp.shared.email.application.EmailSenderPort
import com.evmap.serverapp.shared.email.domain.EmailMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SendEmailToAllUsers(
    private val userReadRepository: UserReadRepository,
    private val emailSender: EmailSenderPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(dto: SendEmailRequest): EmailDeliverySummary {
        val allEmails = userReadRepository.findAllEmails()
        var sent = 0
        for (email in allEmails) {
            try {
                emailSender.send(EmailMessage(to = email, subject = dto.subject, body = dto.body))
                sent++
            } catch (error: Exception) {
                log.warn("SMTP submission failed ({})", error.javaClass.simpleName)
            }
        }
        return EmailDeliverySummary(sent, allEmails.size - sent)
    }
}
