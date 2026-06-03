package com.evmap.serverapp.features.auth.api.controller

import com.evmap.serverapp.features.auth.api.dto.AuthResponse
import com.evmap.serverapp.features.auth.api.dto.LoginRequest
import com.evmap.serverapp.features.auth.api.dto.RefreshTokenRequest
import com.evmap.serverapp.features.auth.api.dto.RegisterRequest
import com.evmap.serverapp.features.auth.application.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse = authService.register(request)

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse = authService.login(request)

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): AuthResponse = authService.refresh(request)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(@Valid @RequestBody request: RefreshTokenRequest) {
        authService.logout(request)
    }
}
