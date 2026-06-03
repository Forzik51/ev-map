package com.evmap.serverapp.features.auth.infra

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class AuthUser(
    val id: Long,
    val username: String,
    val email: String,
    val passwordHash: String,
    val role: String,
)

data class StoredRefreshToken(
    val tokenHash: String,
    val userId: Long,
    val expiresAt: OffsetDateTime,
    val revokedAt: OffsetDateTime?,
)

@Repository
class AuthRepository(
    private val dsl: DSLContext,
) {
    fun findUserByUsernameOrEmail(usernameOrEmail: String): AuthUser? =
        dsl.fetchOne(
            """
            SELECT id, username, email, password_hash, COALESCE(role, 'USER') AS role
            FROM app_user
            WHERE LOWER(username) = LOWER(?)
               OR LOWER(email) = LOWER(?)
            """.trimIndent(),
            usernameOrEmail,
            usernameOrEmail,
        )?.let {
            AuthUser(
                id = it.get("id", Long::class.java)!!,
                username = it.get("username", String::class.java)!!,
                email = it.get("email", String::class.java)!!,
                passwordHash = it.get("password_hash", String::class.java)!!,
                role = it.get("role", String::class.java)!!,
            )
        }

    fun createRefreshToken(userId: Long, expiresAt: OffsetDateTime): String {
        val token = UUID.randomUUID().toString() + UUID.randomUUID().toString()
        dsl.execute(
            """
            INSERT INTO refresh_token(token_hash, user_id, expires_at, created_at)
            VALUES (?, ?, CAST(? AS timestamptz), CAST(? AS timestamptz))
            """.trimIndent(),
            hashToken(token),
            userId,
            expiresAt,
            OffsetDateTime.now(ZoneOffset.UTC),
        )
        return token
    }

    fun findRefreshToken(token: String): StoredRefreshToken? =
        dsl.fetchOne(
            """
            SELECT token_hash, user_id, expires_at, revoked_at
            FROM refresh_token
            WHERE token_hash = ?
            """.trimIndent(),
            hashToken(token),
        )?.let {
            StoredRefreshToken(
                tokenHash = it.get("token_hash", String::class.java)!!,
                userId = it.get("user_id", Long::class.java)!!,
                expiresAt = it.get("expires_at", OffsetDateTime::class.java)!!,
                revokedAt = it.get("revoked_at", OffsetDateTime::class.java),
            )
        }

    fun revokeRefreshToken(token: String) {
        dsl.execute(
            """
            UPDATE refresh_token
            SET revoked_at = CAST(? AS timestamptz)
            WHERE token_hash = ?
              AND revoked_at IS NULL
            """.trimIndent(),
            OffsetDateTime.now(ZoneOffset.UTC),
            hashToken(token),
        )
    }

    fun findUserById(userId: Long): AuthUser? =
        dsl.fetchOne(
            """
            SELECT id, username, email, password_hash, COALESCE(role, 'USER') AS role
            FROM app_user
            WHERE id = ?
            """.trimIndent(),
            userId,
        )?.let {
            AuthUser(
                id = it.get("id", Long::class.java)!!,
                username = it.get("username", String::class.java)!!,
                email = it.get("email", String::class.java)!!,
                passwordHash = it.get("password_hash", String::class.java)!!,
                role = it.get("role", String::class.java)!!,
            )
        }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
