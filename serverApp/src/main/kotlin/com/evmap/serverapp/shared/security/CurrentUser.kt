package com.evmap.serverapp.shared.security

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

data class CurrentUser(
    val id: Long,
    val username: String?,
    val role: String = "USER",
) {
    val isAdmin: Boolean
        get() = role == "ADMIN"
}

@Component
class CurrentUserProvider {
    fun get(): CurrentUser {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw IllegalStateException("Authentication is missing")
        return fromAuthentication(authentication)
    }

    fun fromAuthentication(authentication: Authentication): CurrentUser {
        val principal = authentication.principal
        if (principal is Jwt) {
            val id = principal.subject.toLong()
            return CurrentUser(
                id = id,
                username = principal.getClaimAsString("username"),
                role = principal.getClaimAsString("role") ?: "USER",
            )
        }
        return CurrentUser(id = authentication.name.toLong(), username = null)
    }

    fun requireSelfOrAdmin(targetUserId: Long) {
        val currentUser = get()
        if (currentUser.id != targetUserId && !currentUser.isAdmin) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
        }
    }

    fun requireAdmin() {
        if (!get().isAdmin) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required")
        }
    }
}
