package com.evmap.serverapp.features.auth.application

import com.evmap.serverapp.features.auth.api.dto.AuthResponse
import com.evmap.serverapp.features.auth.api.dto.LoginRequest
import com.evmap.serverapp.features.auth.api.dto.RefreshTokenRequest
import com.evmap.serverapp.features.auth.api.dto.RegisterRequest
import com.evmap.serverapp.features.auth.infra.AuthRepository
import com.evmap.serverapp.features.auth.infra.AuthUser
import com.evmap.serverapp.features.user.application.command.CreateUser
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class AuthService(
    private val authRepository: AuthRepository,
    private val createUser: CreateUser,
    private val jwtTokenService: JwtTokenService,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${app.security.jwt.refresh-token-days:30}") refreshTokenDays: Long,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val refreshTokenTtl: Duration = Duration.ofDays(refreshTokenDays)

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val userId = createUser.handle(request.user)
        val user = authRepository.findUserById(userId) ?: throw InvalidCredentialsException()
        log.info("Registered userId={}", userId)
        return issueTokens(user)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val user = authRepository.findUserByUsernameOrEmail(request.usernameOrEmail)
            ?: throw InvalidCredentialsException()

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            log.warn("Failed login for usernameOrEmail={}", request.usernameOrEmail)
            throw InvalidCredentialsException()
        }

        log.info("Successful login for userId={}", user.id)
        return issueTokens(user)
    }

    @Transactional
    fun refresh(request: RefreshTokenRequest): AuthResponse {
        val stored = authRepository.findRefreshToken(request.refreshToken)
            ?: throw InvalidRefreshTokenException()
        if (stored.revokedAt != null || stored.expiresAt.isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw InvalidRefreshTokenException()
        }

        authRepository.revokeRefreshToken(request.refreshToken)
        val user = authRepository.findUserById(stored.userId) ?: throw InvalidRefreshTokenException()
        log.info("Refreshed token for userId={}", user.id)
        return issueTokens(user)
    }

    fun logout(request: RefreshTokenRequest) {
        authRepository.revokeRefreshToken(request.refreshToken)
    }

    private fun issueTokens(user: AuthUser): AuthResponse {
        val refreshToken = authRepository.createRefreshToken(
            userId = user.id,
            expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plus(refreshTokenTtl),
        )
        return AuthResponse(
            accessToken = jwtTokenService.createAccessToken(user),
            refreshToken = refreshToken,
            expiresInSeconds = jwtTokenService.accessTokenTtl.seconds,
            userId = user.id,
            username = user.username,
        )
    }
}
