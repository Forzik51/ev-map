package com.evmap.serverapp.features.user.infra.write

import com.evmap.serverapp.features.user.api.dto.UpdateProfile
import com.evmap.serverapp.features.user.application.UserNotFoundException
import com.evmap.serverapp.features.user.application.UsernameUnavailableException
import com.evmap.serverapp.features.user.domian.User
import com.evmap.serverapp.features.user.domian.UserRepositoryPort
import com.evmap.serverapp.features.event.domain.EventRepositoryPort
import com.evmap.serverapp.shared.storage.FileStorageService
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class UserRepositoryAdapter(
    private val dsl: DSLContext,
    private val fileStorageService: FileStorageService,
    private val events: EventRepositoryPort,
) : UserRepositoryPort {

    override fun save(user: User): Long =
        dsl.fetchOne(
            """
            INSERT INTO app_user(name, surname, email, phone, birthdate, password_hash, username, page_description, path)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """.trimIndent(),
            user.name,
            user.surname,
            user.email,
            user.phone,
            user.birthdate,
            user.passwordHash,
            user.username,
            user.pageDescription,
            user.path
        )?.get("id", Long::class.java) ?: error("Failed to create user")

    @Transactional
    override fun delete(userId: Long) {
        val profilePath = lockUserAndFindPath(userId)

        dsl.fetch("SELECT id FROM event WHERE user_id = ? ORDER BY id", userId)
            .map { it.get("id", Long::class.javaObjectType)!! }
            .forEach(events::delete)

        dsl.execute("DELETE FROM search_history WHERE user_id = ?", userId)
        dsl.execute("DELETE FROM rating WHERE user_id = ?", userId)
        dsl.execute("DELETE FROM repost WHERE user_id = ?", userId)
        dsl.execute(
            """
            UPDATE comment SET parent_comment_id = NULL
            WHERE user_id <> ? AND parent_comment_id IN (SELECT id FROM comment WHERE user_id = ?)
            """.trimIndent(),
            userId, userId,
        )
        dsl.execute("DELETE FROM comment WHERE user_id = ?", userId)

        dsl.execute("DELETE FROM follow WHERE follower_id = ? OR following_id = ?", userId, userId)
        dsl.execute("DELETE FROM message WHERE user_id = ?", userId)
        dsl.execute("DELETE FROM user_chat WHERE user_id = ?", userId)

        dsl.execute(
            "DELETE FROM event_list WHERE favourite_list_id IN (SELECT id FROM user_list WHERE user_id = ?)",
            userId
        )
        dsl.execute("DELETE FROM user_list WHERE user_id = ?", userId)

        dsl.execute("DELETE FROM report WHERE user_id = ?", userId)
        dsl.execute("DELETE FROM advertising WHERE company_id IN (SELECT id FROM company WHERE user_id = ?)", userId)
        dsl.execute("DELETE FROM company WHERE user_id = ?", userId)
        dsl.execute("DELETE FROM moderator WHERE user_id = ?", userId)

        dsl.execute("DELETE FROM app_user WHERE id = ?", userId)
        profilePath?.let { fileStorageService.deleteAfterCommit(listOf(it)) }
    }

    override fun updateDescription(userId: Long, description: String) {
        val updated = dsl.execute("UPDATE app_user SET page_description = ? WHERE id = ?", description, userId)
        if (updated == 0) throw UserNotFoundException(userId)
    }

    override fun updateProfile(userId: Long, profile: UpdateProfile) {
        val updated = dsl.execute(
            """
            UPDATE app_user u SET name = ?, surname = ?, username = ?, phone = ?, page_description = ?, birthdate = CASE WHEN ? THEN CAST(? AS date) ELSE birthdate END
            WHERE u.id = ? AND (LOWER(u.username) = LOWER(?) OR NOT EXISTS (
                SELECT 1 FROM app_user other WHERE other.id <> u.id AND LOWER(other.email) = LOWER(?)
            ))
            """.trimIndent(),
            profile.name, profile.surname, profile.username, com.evmap.serverapp.shared.security.normalizedPhone(profile.phone), profile.pageDescription, profile.updateDateOfBirth, profile.dateOfBirth, userId,
            profile.username, profile.username,
        )
        if (updated == 0) {
            ensureUserExists(userId)
            throw UsernameUnavailableException()
        }
    }

    override fun updatePath(userId: Long, path: String?) {
        val updated = dsl.execute("UPDATE app_user SET path = ? WHERE id = ?", path, userId)
        if (updated == 0) throw UserNotFoundException(userId)
    }

    override fun findPathByUserId(userId: Long): String? = findPath(userId, forUpdate = false)

    override fun lockUserAndFindPath(userId: Long): String? = findPath(userId, forUpdate = true)

    private fun findPath(userId: Long, forUpdate: Boolean): String? {
        val lock = if (forUpdate) " FOR UPDATE" else ""
        val record = dsl.fetchOne("SELECT path FROM app_user WHERE id = ?$lock", userId)
            ?: throw UserNotFoundException(userId)
        return record.get("path", String::class.java)
    }

    override fun follow(userId: Long, targetUserId: Long) {
        ensureUserExists(userId)
        ensureUserExists(targetUserId)

        dsl.execute(
            """
            INSERT INTO follow(follower_id, following_id)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """.trimIndent(),
            userId,
            targetUserId
        )
    }

    override fun unfollow(userId: Long, targetUserId: Long) {
        ensureUserExists(userId)
        ensureUserExists(targetUserId)
        dsl.execute("DELETE FROM follow WHERE follower_id = ? AND following_id = ?", userId, targetUserId)
    }

    private fun ensureUserExists(userId: Long) {
        val exists = dsl.fetchOne("SELECT 1 FROM app_user WHERE id = ?", userId) != null
        if (!exists) throw UserNotFoundException(userId)
    }
}
