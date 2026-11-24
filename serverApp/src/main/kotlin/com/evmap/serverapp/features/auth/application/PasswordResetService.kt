package com.evmap.serverapp.features.auth.application

import com.evmap.serverapp.config.PasswordResetProperties
import com.evmap.serverapp.features.auth.api.dto.*
import com.evmap.serverapp.features.auth.infra.AuthRepository
import com.evmap.serverapp.features.auth.infra.PasswordResetRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.OffsetDateTime
import java.util.Locale
import java.util.UUID

@Service
class PasswordResetService(
    private val repository: PasswordResetRepository,
    private val users: AuthRepository,
    private val crypto: PasswordResetCrypto,
    private val passwordEncoder: PasswordEncoder,
    private val properties: PasswordResetProperties,
    private val clock: Clock,
    private val events: ApplicationEventPublisher,
) {
    @Transactional
    fun forgot(request: ForgotPasswordRequest, clientIp: String): ForgotPasswordResponse {
        val now = OffsetDateTime.now(clock)
        val email = request.email.trim().lowercase(Locale.ROOT)
        val emailHash = crypto.identifierHash("email:$email")
        repository.checkRequestLimits(emailHash, crypto.identifierHash("ip:$clientIp"), now, properties)
        val user = users.findUserByEmailForUpdate(email)
        val id = UUID.randomUUID()
        val code = crypto.newCode()
        val expiresAt = now.plus(properties.codeTtl)
        repository.create(id, user?.id, emailHash, crypto.identifierHash("ip:$clientIp"), crypto.codeHash(id, code), now, expiresAt)
        repository.enqueueMail(
            id, user?.id, user?.email ?: email, "Your EvMap password recovery code",
            crypto.encrypt("Your recovery code is: $code\n\nIt expires in ${properties.codeTtl.toMinutes()} minutes.\nIf you did not request this, you can ignore this email."),
            crypto.keyId, now, expiresAt,
        )
        return ForgotPasswordResponse(id, properties.resendCooldown.seconds)
    }

    // Persist failed attempts even though the API returns an error.
    @Transactional(noRollbackFor = [InvalidPasswordResetCode::class])
    fun verify(request: VerifyPasswordCodeRequest): VerifyPasswordCodeResponse {
        val now = OffsetDateTime.now(clock)
        val stored = repository.findForUpdate(request.requestId) ?: throw InvalidPasswordResetCode()
        if (stored.userId == null || stored.usedAt != null || stored.invalidatedAt != null || stored.verifiedAt != null ||
            !stored.expiresAt.isAfter(now) || stored.failedAttempts >= properties.maxAttempts) throw InvalidPasswordResetCode()
        if (!crypto.matchesCode(stored.id, request.code, stored.codeHash)) {
            repository.incrementAttempts(stored.id)
            throw InvalidPasswordResetCode()
        }
        val token = crypto.newToken()
        repository.markVerified(stored.id, crypto.tokenHash(token), now, now.plus(properties.resetTokenTtl))
        return VerifyPasswordCodeResponse(token, properties.resetTokenTtl.seconds)
    }

    @Transactional
    fun reset(request: ResetPasswordRequest) {
        val hash = crypto.tokenHash(request.resetToken)
        val candidate = repository.findByTokenHash(hash) ?: throw InvalidPasswordResetToken()
        val userId = candidate.userId ?: throw InvalidPasswordResetToken()
        // Login and refresh also lock this user: no new session can race the revocation.
        val user = users.findUserByIdForUpdate(userId) ?: throw InvalidPasswordResetToken()
        val stored = repository.findForUpdate(candidate.id) ?: throw InvalidPasswordResetToken()
        val now = OffsetDateTime.now(clock)
        if (stored.usedAt != null || stored.invalidatedAt != null || stored.verifiedAt == null ||
            stored.resetExpiresAt?.isAfter(now) != true) throw InvalidPasswordResetToken()
        repository.complete(userId, stored.id, passwordEncoder.encode(request.newPassword), now)
        repository.enqueueMail(
            null, userId, user.email, "Your EvMap password has been changed",
            crypto.encrypt("Your password has been changed and your existing sessions have been signed out.\nIf this was not you, recover your account immediately."),
            crypto.keyId, now, now.plusDays(1),
        )
        events.publishEvent(PasswordChangedEvent(userId))
    }
}
