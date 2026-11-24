package com.evmap.serverapp.features.notification

import com.evmap.serverapp.features.notification.api.dto.EmailDeliverySummary
import com.evmap.serverapp.features.notification.api.dto.SendEmailRequest
import com.evmap.serverapp.features.notification.application.command.SendEmailToAllUsers
import com.evmap.serverapp.features.user.infra.read.UserReadRepository
import com.evmap.serverapp.shared.email.application.EmailSenderPort
import com.evmap.serverapp.shared.email.domain.EmailMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.mail.MailSendException

class SendEmailToAllUsersTest {
    @Test
    fun `SMTP results count successful submissions and individual failures`() {
        val addresses = listOf("one@example.invalid", "two@example.invalid")
        val users = Mockito.mock(UserReadRepository::class.java)
        Mockito.`when`(users.findAllEmails()).thenReturn(addresses)
        for (failed in 0..addresses.size) {
            val attempts = mutableListOf<String>()
            val sender = object : EmailSenderPort {
                override fun send(message: EmailMessage) {
                    attempts += message.to
                    if (attempts.size <= failed) throw MailSendException("Simulated SMTP failure")
                }
            }
            val summary = SendEmailToAllUsers(users, sender).handle(SendEmailRequest("Subject", "Body"))
            assertEquals(EmailDeliverySummary(addresses.size - failed, failed), summary)
            assertEquals(addresses, attempts)
        }
    }
}
