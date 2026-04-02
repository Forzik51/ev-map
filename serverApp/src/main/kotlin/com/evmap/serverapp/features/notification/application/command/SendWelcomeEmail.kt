package com.evmap.serverapp.features.notification.application.command

import com.evmap.serverapp.shared.email.application.EmailSenderPort
import com.evmap.serverapp.shared.email.domain.EmailMessage
import org.springframework.stereotype.Service

@Service
class SendWelcomeEmail(
    private val emailSender: EmailSenderPort,
) {
    fun handle(email: String, username: String) {
        emailSender.send(
            EmailMessage(
                to = email,
                subject = "Welcome to EvMap",
                body =
                    """
                    Hi $username,
                    
                    Welcome to EvMap. Your account is ready.
                    """.trimIndent(),
            )
        )
    }
}
