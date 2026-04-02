package com.evmap.serverapp.shared.email.infra

import com.evmap.serverapp.shared.email.application.EmailSenderPort
import com.evmap.serverapp.shared.email.domain.EmailMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service

@Service
class SmtpEmailSenderAdapter(
    private val mailSender: JavaMailSender,
    @Value("\${app.mail.from}") private val fromAddress: String,
) : EmailSenderPort {
    override fun send(message: EmailMessage) {
        val email = SimpleMailMessage().apply {
            from = fromAddress
            setTo(message.to)
            subject = message.subject
            text = message.body
        }
        mailSender.send(email)
    }
}
