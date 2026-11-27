package com.evmap.serverapp.features.event.infra.write

import com.evmap.serverapp.features.event.application.EventNotFoundException
import com.evmap.serverapp.features.event.domain.Event
import com.evmap.serverapp.features.event.domain.EventRepositoryPort
import com.evmap.serverapp.shared.storage.FileStorageService
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class EventRepositoryAdapter(
    private val jpa: SpringDataEventJpa,
    private val dsl: DSLContext,
    private val fileStorageService: FileStorageService,
) : EventRepositoryPort {
    @Transactional
    override fun save(event: Event): Long {
        ensureLocation(event.locationId)
        val e = jpa.save(
            EventEntity(
                name = event.name,
                description = event.description,
                startsAt = event.startsAt,
                endsAt = event.endsAt,
                locationId = event.locationId,
                userId = event.userId,
                createdAt = event.createdAt,
                dateMode = event.dateMode, startsOn = event.startsOn, endsOn = event.endsOn, timeZone = event.timeZone,
            )
        )
        val eventId = e.id!!
        replaceEventCategories(eventId, event.categoryIds)
        replaceTags(eventId,event.tags)
        return eventId
    }

    @Transactional
    override fun update(eventId: Long, event: Event) {
        ensureLocation(event.locationId)
        val updated = dsl.execute(
            """
            UPDATE event
            SET name = ?,
                description = ?,
                start_at = CAST(? AS timestamptz),
                end_at = CAST(? AS timestamptz),
                location_id = ?,
                user_id = ?, date_mode = ?, starts_on = CAST(? AS date), ends_on = CAST(? AS date), time_zone = ?
            WHERE id = ?
            """.trimIndent(),
            event.name,
            event.description,
            event.startsAt?.toOffsetDateTimeUtc(),
            event.endsAt?.toOffsetDateTimeUtc(),
            event.locationId,
            event.userId, event.dateMode, event.startsOn, event.endsOn, event.timeZone,
            eventId
        )
        if (updated == 0) throw EventNotFoundException(eventId)
        replaceEventCategories(eventId, event.categoryIds)
        replaceTags(eventId,event.tags)
    }

    @Transactional
    override fun delete(eventId: Long) {
        if (dsl.fetchOne("SELECT id FROM event WHERE id = ? FOR UPDATE", eventId) == null) {
            throw EventNotFoundException(eventId)
        }
        val photoPaths = dsl.fetch(
            """
            SELECT image_path
            FROM photo
            WHERE event_id = ?
            """.trimIndent(),
            eventId
        ).mapNotNull { it.get("image_path", String::class.java) }

        dsl.execute("DELETE FROM report WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM advertising WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM repost WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM rating WHERE event_id = ?", eventId)
        // Imported comments can refer to a parent on a different event.
        dsl.execute(
            """
            UPDATE comment SET parent_comment_id = NULL
            WHERE event_id <> ?
              AND parent_comment_id IN (SELECT id FROM comment WHERE event_id = ?)
            """.trimIndent(),
            eventId, eventId,
        )
        dsl.execute("DELETE FROM comment WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM category_event WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM event_list WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM photo WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM event WHERE id = ?", eventId)
        fileStorageService.deleteAfterCommit(photoPaths)
    }

    override fun addComment(eventId: Long, userId: Long, comment: String) {
        ensureEventExists(eventId)
        dsl.execute(
            """
            INSERT INTO comment(text, event_id, parent_comment_id, comment_at, user_id)
            VALUES (?, ?, ?, CAST(? AS timestamptz), ?)
            """.trimIndent(),
            comment,
            eventId,
            null,
            OffsetDateTime.now(ZoneOffset.UTC),
            userId
        )
    }

    override fun addReaction(eventId: Long, userId: Long, reaction: String) {
        ensureEventExists(eventId)
        val rating = parseReaction(reaction)
        dsl.execute(
            """
            INSERT INTO rating(rating, event_id, user_id)
            VALUES (?, ?, ?)
            ON CONFLICT (event_id, user_id)
            DO UPDATE SET rating = EXCLUDED.rating
            """.trimIndent(),
            rating,
            eventId,
            userId
        )
    }

    override fun addShare(eventId: Long, userId: Long) {
        ensureEventExists(eventId)
        dsl.execute(
            """
            INSERT INTO repost(event_id, user_id)
            VALUES (?, ?)
            ON CONFLICT DO NOTHING
            """.trimIndent(),
            eventId,
            userId
        )
    }

    override fun removeShare(eventId: Long, userId: Long) {
        ensureEventExists(eventId)
        dsl.execute("DELETE FROM repost WHERE event_id = ? AND user_id = ?", eventId, userId)
    }

    private fun ensureLocation(id: Long) {
        if (dsl.fetchOne("SELECT 1 FROM location WHERE id=?",id) == null) throw com.evmap.serverapp.shared.api.FormValidationException(mapOf("locationId" to "Виберіть наявне місце"))
    }
    private fun replaceTags(eventId: Long, tags: List<String>) {
        dsl.execute("DELETE FROM event_tag WHERE event_id=?",eventId)
        tags.distinctBy { com.evmap.serverapp.features.event.application.normalizedTag(it) }.forEach { name ->
            val normalized = com.evmap.serverapp.features.event.application.normalizedTag(name)
            val id = dsl.fetchOne("INSERT INTO tag(name,normalized_name) VALUES(?,?) ON CONFLICT(normalized_name) DO UPDATE SET normalized_name=excluded.normalized_name RETURNING id",name,normalized)!!.get(0,Long::class.java)!!
            dsl.execute("INSERT INTO event_tag(event_id,tag_id) VALUES(?,?)",eventId,id)
        }
    }

    private fun ensureEventExists(eventId: Long) {
        val exists = dsl.fetchOne("SELECT 1 FROM event WHERE id = ?", eventId) != null
        if (!exists) throw EventNotFoundException(eventId)
    }

    private fun parseReaction(reaction: String): BigDecimal {
        val normalized = reaction.trim().lowercase()
        val score = normalized.toBigDecimalOrNull()
            ?: when (normalized) {
                "like", "love", "heart", "upvote" -> BigDecimal("5.0")
                "dislike", "downvote" -> BigDecimal("1.0")
                "neutral" -> BigDecimal("3.0")
                else -> throw IllegalArgumentException("Rating must be a number from 0 to 5")
            }

        require(score >= BigDecimal.ZERO && score <= BigDecimal("5.0")) { "Rating must be from 0 to 5" }
        return score.setScale(1, RoundingMode.HALF_UP)
    }

    private fun replaceEventCategories(eventId: Long, rawCategoryIds: List<Long>) {
        val categoryIds = rawCategoryIds.distinct()
        if (categoryIds.isEmpty()) {
            throw IllegalArgumentException("Event must contain at least one category")
        }

        ensureCategoriesExist(categoryIds)
        dsl.execute("DELETE FROM category_event WHERE event_id = ?", eventId)
        categoryIds.forEach { categoryId ->
            dsl.execute(
                """
                INSERT INTO category_event(category_id, event_id)
                VALUES (?, ?)
                """.trimIndent(),
                categoryId,
                eventId
            )
        }
    }

    private fun ensureCategoriesExist(categoryIds: List<Long>) {
        val placeholders = categoryIds.joinToString(",") { "?" }
        val sql = """
            SELECT id
            FROM category
            WHERE id IN ($placeholders)
        """.trimIndent()
        val found = dsl.fetch(sql, *categoryIds.toTypedArray())
            .mapNotNull { it.get("id", Long::class.java) }
            .toSet()

        if (found.size != categoryIds.size) {
            val missing = categoryIds.filterNot { it in found }
            throw IllegalArgumentException("Unknown category ids: $missing")
        }
    }

    private fun Instant.toOffsetDateTimeUtc(): OffsetDateTime = atOffset(ZoneOffset.UTC)
}
