package com.evmap.serverapp.features.user.infra.read

import com.evmap.serverapp.features.user.api.dto.ViewUser
import com.evmap.serverapp.features.user.api.dto.ViewUserShort
import com.evmap.serverapp.features.user.application.UserNotFoundException
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.sql.Date

@Repository
class UserReadRepository(
    private val dsl: DSLContext
) {
    fun findAll(): List<ViewUser> =
        dsl.fetch(
            """
            SELECT u.id,
                   u.name,
                   u.surname,
                   u.email,
                   u.phone,
                   u.birthdate,
                   u.username,
                   u.page_description,
                   (SELECT COUNT(*) FROM follow f WHERE f.following_id = u.id) AS followers_count,
                   (SELECT COUNT(*) FROM follow f WHERE f.follower_id = u.id) AS following_count
            FROM app_user u
            ORDER BY u.id DESC
            """.trimIndent()
        ).map { toViewUser(it) }

    fun findById(userId: Long): ViewUser =
        dsl.fetchOne(
            """
            SELECT u.id,
                   u.name,
                   u.surname,
                   u.email,
                   u.phone,
                   u.birthdate,
                   u.username,
                   u.page_description,
                   (SELECT COUNT(*) FROM follow f WHERE f.following_id = u.id) AS followers_count,
                   (SELECT COUNT(*) FROM follow f WHERE f.follower_id = u.id) AS following_count
            FROM app_user u
            WHERE u.id = ?
            """.trimIndent(),
            userId
        )?.let { toViewUser(it) } ?: throw UserNotFoundException(userId)

    fun findFollowers(userId: Long): List<ViewUserShort> {
        ensureUserExists(userId)
        return dsl.fetch(
            """
            SELECT u.id,
                   u.username,
                   u.page_description
            FROM follow f0
            JOIN app_user u ON u.id = f0.follower_id
            WHERE f0.following_id = ?
            ORDER BY u.username
            """.trimIndent(),
            userId
        ).map { toViewUserShort(it) }
    }

    fun findFollowing(userId: Long): List<ViewUserShort> {
        ensureUserExists(userId)
        return dsl.fetch(
            """
            SELECT u.id,
                   u.username,
                   u.page_description
            FROM follow f0
            JOIN app_user u ON u.id = f0.following_id
            WHERE f0.follower_id = ?
            ORDER BY u.username
            """.trimIndent(),
            userId
        ).map { toViewUserShort(it) }
    }

    private fun ensureUserExists(userId: Long) {
        val exists = dsl.fetchOne("SELECT 1 FROM app_user WHERE id = ?", userId) != null
        if (!exists) throw UserNotFoundException(userId)
    }

    private fun toViewUser(record: org.jooq.Record): ViewUser =
        ViewUser(
            id = record.get("id", Long::class.java)!!,
            name = record.get("name", String::class.java)!!,
            surname = record.get("surname", String::class.java),
            email = record.get("email", String::class.java)!!,
            phone = record.get("phone", String::class.java)!!,
            birthdate = record.get("birthdate", Date::class.java)!!.toLocalDate(),
            username = record.get("username", String::class.java)!!,
            pageDescription = record.get("page_description", String::class.java)!!,
            followersCount = record.get("followers_count", Int::class.java) ?: 0,
            followingCount = record.get("following_count", Int::class.java) ?: 0,
        )

    private fun toViewUserShort(record: org.jooq.Record): ViewUserShort =
        ViewUserShort(
            id = record.get("id", Long::class.java)!!,
            username = record.get("username", String::class.java)!!,
            pageDescription = record.get("page_description", String::class.java)!!,
        )
}
