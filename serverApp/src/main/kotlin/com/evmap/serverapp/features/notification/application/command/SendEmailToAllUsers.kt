package com.evmap.serverapp.features.notification.application.command

import com.evmap.serverapp.features.notification.api.dto.SendEmailRequest
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

    fun handle(dto: SendEmailRequest) {
        val allEmails = userReadRepository.findAllEmails()
        val totalBatches = allEmails.size.let { if (it == 0) 0 else ((it - 1) / BATCH_SIZE) + 1 }

        allEmails
            .chunked(BATCH_SIZE)
            .forEachIndexed { batchIndex, emails ->
                log.info("Sending email batch {}/{} with {} recipients", batchIndex + 1, totalBatches, emails.size)

                emails.forEach { email ->
                    runCatching {
                        emailSender.send(
                            EmailMessage(
                                to = email,
                                subject = dto.subject,
                                body = dto.body,
                            )
                        )
                    }.onFailure { ex ->
                        log.warn("Failed to send email to recipient={}", email, ex)
                    }
                }
            }
    }

    companion object {
        private const val BATCH_SIZE = 100
    }
}
