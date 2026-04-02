package com.evmap.serverapp.features.user.application.command

import com.evmap.serverapp.features.user.api.dto.CreateUser as CreateUserRequest
import com.evmap.serverapp.features.notification.application.command.SendWelcomeEmail
import com.evmap.serverapp.features.user.domian.User
import com.evmap.serverapp.features.user.domian.UserRepositoryPort
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class CreateUser(
    private val repo: UserRepositoryPort,
    private val sendWelcomeEmail: SendWelcomeEmail,
) {
    private val passwordEncoder = BCryptPasswordEncoder()
    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(dto: CreateUserRequest): Long {
        val userId = repo.save(
            User(
                name = dto.name,
                surname = dto.surname,
                email = dto.email,
                phone = dto.phone,
                birthdate = dto.dateOfBirth,
                passwordHash = passwordEncoder.encode(dto.password),
                username = dto.username,
                pageDescription = dto.pageDescription,
            )
        )

        runCatching { sendWelcomeEmail.handle(dto.email, dto.username) }
            .onFailure { ex -> log.warn("Failed to send welcome email for userId={}", userId, ex) }

        return userId
    }
}
