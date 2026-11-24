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
    val authVersion: Long = 0,
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
            SELECT id, username, email, password_hash, COALESCE(role, 'USER') AS role, auth_version
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
                authVersion = it.get("auth_version", Long::class.java)!!,
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
            SELECT id, username, email, password_hash, COALESCE(role, 'USER') AS role, auth_version
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
                authVersion = it.get("auth_version", Long::class.java)!!,
            )
        }

    fun findUserByIdForUpdate(userId: Long): AuthUser? =
        dsl.fetchOne("SELECT * FROM app_user WHERE id = ? FOR UPDATE", userId)?.let(::toAuthUser)

    fun findUserByEmailForUpdate(email: String): AuthUser? =
        dsl.fetchOne("SELECT * FROM app_user WHERE LOWER(email) = ? FOR UPDATE", email)?.let(::toAuthUser)

    fun currentAuthVersion(userId: Long): Long? =
        dsl.fetchOne("SELECT auth_version FROM app_user WHERE id = ?", userId)?.get("auth_version", Long::class.java)

    fun isChatMember(chatId: Long, userId: Long): Boolean =
        dsl.fetchOne("SELECT 1 FROM user_chat WHERE chat_id = ? AND user_id = ?", chatId, userId) != null

    private fun toAuthUser(row: org.jooq.Record) = AuthUser(
        id = row.get("id", Long::class.java)!!,
        username = row.get("username", String::class.java)!!,
        email = row.get("email", String::class.java)!!,
        passwordHash = row.get("password_hash", String::class.java)!!,
        role = row.get("role", String::class.java) ?: "USER",
        authVersion = row.get("auth_version", Long::class.java)!!,
    )

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
