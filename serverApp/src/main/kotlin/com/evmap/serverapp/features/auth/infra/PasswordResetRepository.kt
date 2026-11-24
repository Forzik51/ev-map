package com.evmap.serverapp.features.auth.infra

import com.evmap.serverapp.config.PasswordResetProperties
import com.evmap.serverapp.features.auth.application.PasswordResetRateLimited
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.UUID

data class PasswordResetRecord(
    val id: UUID,
    val userId: Long?,
    val codeHash: String,
    val expiresAt: OffsetDateTime,
    val failedAttempts: Int,
    val verifiedAt: OffsetDateTime?,
    val resetExpiresAt: OffsetDateTime?,
    val usedAt: OffsetDateTime?,
    val invalidatedAt: OffsetDateTime?,
)

@Repository
class PasswordResetRepository(private val dsl: DSLContext) {
    // Every writer takes the same database locks, including requests for unknown addresses.
    fun checkRequestLimits(emailHash: String, ipHash: String, now: OffsetDateTime, properties: PasswordResetProperties) {
        listOf(emailHash, ipHash).map { it.take(16).toULong(16).toLong() }.distinct().sorted().forEach {
            dsl.fetch("SELECT pg_advisory_xact_lock(?)", it)
        }
        checkLimit("identifier_hash", emailHash, now, properties.requestsPerEmailPerHour, properties.resendCooldown.seconds)
        checkLimit("ip_hash", ipHash, now, properties.requestsPerIpPerHour, 0)
    }

    private fun checkLimit(column: String, hash: String, now: OffsetDateTime, maximum: Int, cooldownSeconds: Long) {
        val record = dsl.fetchOne(
            "SELECT COUNT(*) AS n, MAX(created_at) AS last_at FROM password_reset_request WHERE $column = ? AND created_at > CAST(? AS timestamptz)",
            hash, now.minusHours(1),
        )!!
        val count = record.get("n", Int::class.java) ?: 0
        val last = record.get("last_at", OffsetDateTime::class.java)
        if (count >= maximum) throw PasswordResetRateLimited(3600)
        if (cooldownSeconds > 0 && last != null && last.plusSeconds(cooldownSeconds).isAfter(now)) {
            throw PasswordResetRateLimited(
                ((java.time.Duration.between(now, last.plusSeconds(cooldownSeconds)).toMillis() + 999) / 1000).coerceAtLeast(1),
            )
        }
    }

    fun create(id: UUID, userId: Long?, emailHash: String, ipHash: String, codeHash: String, now: OffsetDateTime, expiresAt: OffsetDateTime) {
        dsl.execute(
            """UPDATE password_reset_request SET invalidated_at = CAST(? AS timestamptz)
               WHERE identifier_hash = ? AND used_at IS NULL AND invalidated_at IS NULL""",
            now, emailHash,
        )
        dsl.execute(
            """INSERT INTO password_reset_request(id, user_id, identifier_hash, ip_hash, code_hash, created_at, expires_at)
               VALUES (?, ?, ?, ?, ?, CAST(? AS timestamptz), CAST(? AS timestamptz))""",
            id, userId, emailHash, ipHash, codeHash, now, expiresAt,
        )
    }

    fun findForUpdate(id: UUID): PasswordResetRecord? =
        dsl.fetchOne("SELECT * FROM password_reset_request WHERE id = ? FOR UPDATE", id)?.let(::map)

    fun findByTokenHash(hash: String): PasswordResetRecord? =
        dsl.fetchOne("SELECT * FROM password_reset_request WHERE reset_token_hash = ?", hash)?.let(::map)

    fun incrementAttempts(id: UUID) {
        dsl.execute("UPDATE password_reset_request SET failed_attempts = failed_attempts + 1 WHERE id = ?", id)
    }

    fun markVerified(id: UUID, tokenHash: String, now: OffsetDateTime, expiresAt: OffsetDateTime) {
        dsl.execute(
            "UPDATE password_reset_request SET verified_at = CAST(? AS timestamptz), reset_token_hash = ?, reset_expires_at = CAST(? AS timestamptz) WHERE id = ?",
            now, tokenHash, expiresAt, id,
        )
    }

    fun complete(userId: Long, requestId: UUID, passwordHash: String, now: OffsetDateTime) {
        dsl.execute("UPDATE app_user SET password_hash = ?, auth_version = auth_version + 1 WHERE id = ?", passwordHash, userId)
        dsl.execute("UPDATE refresh_token SET revoked_at = CAST(? AS timestamptz) WHERE user_id = ? AND revoked_at IS NULL", now, userId)
        dsl.execute("UPDATE password_reset_request SET invalidated_at = CAST(? AS timestamptz) WHERE user_id = ? AND invalidated_at IS NULL", now, userId)
        dsl.execute("UPDATE password_reset_request SET used_at = CAST(? AS timestamptz) WHERE id = ?", now, requestId)
    }

    fun enqueueMail(requestId: UUID?, userId: Long?, recipient: String, subject: String, encryptedBody: String, keyId: String,
                    now: OffsetDateTime, expiresAt: OffsetDateTime) {
        dsl.execute(
            """INSERT INTO password_reset_mail_outbox
               (id, request_id, user_id, recipient, subject, encrypted_body, key_id, available_at, expires_at)
               SELECT ?, ?, ?, ?, ?, ?, ?, CAST(? AS timestamptz), CAST(? AS timestamptz) WHERE CAST(? AS bigint) IS NOT NULL""",
            UUID.randomUUID(), requestId, userId, recipient, subject, encryptedBody, keyId, now, expiresAt, userId,
        )
    }

    private fun map(row: Record) = PasswordResetRecord(
        id = row.get("id", UUID::class.java)!!,
        userId = row.get("user_id", Long::class.java),
        codeHash = row.get("code_hash", String::class.java)!!,
        expiresAt = row.get("expires_at", OffsetDateTime::class.java)!!,
        failedAttempts = row.get("failed_attempts", Int::class.java)!!,
        verifiedAt = row.get("verified_at", OffsetDateTime::class.java),
        resetExpiresAt = row.get("reset_expires_at", OffsetDateTime::class.java),
        usedAt = row.get("used_at", OffsetDateTime::class.java),
        invalidatedAt = row.get("invalidated_at", OffsetDateTime::class.java),
    )
}
