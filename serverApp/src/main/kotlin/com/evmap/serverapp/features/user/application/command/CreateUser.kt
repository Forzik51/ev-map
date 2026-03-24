package com.evmap.serverapp.features.user.application.command

import com.evmap.serverapp.features.user.api.dto.CreateUser as CreateUserRequest
import com.evmap.serverapp.features.user.domian.User
import com.evmap.serverapp.features.user.domian.UserRepositoryPort
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class CreateUser(private val repo: UserRepositoryPort) {
    private val passwordEncoder = BCryptPasswordEncoder()

    fun handle(dto: CreateUserRequest): Long =
        repo.save(
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
}
