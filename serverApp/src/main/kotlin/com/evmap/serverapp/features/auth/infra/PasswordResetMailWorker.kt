package com.evmap.serverapp.features.auth.infra

import com.evmap.serverapp.config.PasswordResetProperties
import com.evmap.serverapp.features.auth.application.PasswordResetCrypto
import com.evmap.serverapp.shared.email.application.EmailSenderPort
import com.evmap.serverapp.shared.email.domain.EmailMessage
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.OffsetDateTime
import java.util.UUID

@Component
class PasswordResetMailWorker(
    private val dsl: DSLContext,
    private val transactions: TransactionTemplate,
    private val crypto: PasswordResetCrypto,
    private val emailSender: EmailSenderPort,
    private val properties: PasswordResetProperties,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${app.security.password-reset.mail-poll-ms:5000}")
    fun deliverPending() {
        repeat(5) { if (transactions.execute { deliverOne() } != true) return }
    }

    private fun deliverOne(): Boolean {
        val now = OffsetDateTime.now(clock)
        val row = dsl.fetchOne(
            """SELECT * FROM password_reset_mail_outbox
               WHERE sent_at IS NULL AND failed_at IS NULL AND available_at <= CAST(? AS timestamptz)
               ORDER BY available_at, id LIMIT 1 FOR UPDATE SKIP LOCKED""", now,
        ) ?: return false
        val id = row.get("id", UUID::class.java)!!
        val requestId = row.get("request_id", UUID::class.java)
        val requestActive = requestId == null || dsl.fetchOne(
            """SELECT 1 FROM password_reset_request WHERE id = ? AND used_at IS NULL
               AND invalidated_at IS NULL AND verified_at IS NULL AND expires_at > CAST(? AS timestamptz)""", requestId, now,
        ) != null
        if (!requestActive || !row.get("expires_at", OffsetDateTime::class.java)!!.isAfter(now) ||
            row.get("key_id", String::class.java) != crypto.keyId) {
            dsl.execute("UPDATE password_reset_mail_outbox SET failed_at = CAST(? AS timestamptz) WHERE id = ?", now, id)
            return true
        }
        val attempts = row.get("attempts", Int::class.java)!! + 1
        try {
            emailSender.send(EmailMessage(
                to = row.get("recipient", String::class.java)!!,
                subject = row.get("subject", String::class.java)!!,
                body = crypto.decrypt(row.get("encrypted_body", String::class.java)!!),
            ))
            dsl.execute("UPDATE password_reset_mail_outbox SET attempts = ?, sent_at = CAST(? AS timestamptz) WHERE id = ?", attempts, now, id)
        } catch (ex: Exception) {
            // Mail exceptions can contain recipient details. Never log the body, code, or token.
            log.warn("Password recovery email delivery failed (attempt {})", attempts)
            dsl.execute(
                "UPDATE password_reset_mail_outbox SET attempts = ?, available_at = CAST(? AS timestamptz), failed_at = CAST(? AS timestamptz) WHERE id = ?",
                attempts, now.plusSeconds(30L * attempts), if (attempts >= properties.mailMaxAttempts) now else null, id,
            )
        }
        return true
    }

    @Scheduled(fixedDelayString = "\${app.security.password-reset.cleanup-ms:3600000}")
    fun cleanup() {
        val cutoff = OffsetDateTime.now(clock).minusDays(7)
        dsl.execute("DELETE FROM password_reset_request WHERE created_at < CAST(? AS timestamptz)", cutoff)
        dsl.execute("DELETE FROM password_reset_mail_outbox WHERE expires_at < CAST(? AS timestamptz)", cutoff)
    }
}
