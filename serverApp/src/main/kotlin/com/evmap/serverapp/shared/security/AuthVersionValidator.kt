package com.evmap.serverapp.shared.security

import com.evmap.serverapp.features.auth.infra.AuthRepository
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class AuthVersionValidator(private val repository: AuthRepository) : OAuth2TokenValidator<Jwt> {
    override fun validate(token: Jwt): OAuth2TokenValidatorResult {
        val userId = token.subject?.toLongOrNull()
        // Legacy tokens are valid only until the first password reset (version 0).
        val version = (token.claims["authVersion"] as? Number)?.toLong() ?: 0L
        return if (userId != null && repository.currentAuthVersion(userId) == version) {
            OAuth2TokenValidatorResult.success()
        } else {
            OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", "Session has been revoked", null))
        }
    }
}
