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
        val e = jpa.save(
            EventEntity(
                name = event.name,
                description = event.description,
                startsAt = event.startsAt,
                endsAt = event.endsAt,
                locationId = event.locationId,
                userId = event.userId,
                createdAt = event.createdAt,
            )
        )
        val eventId = e.id!!
        replaceEventCategories(eventId, event.categoryIds)
        return eventId
    }

    @Transactional
    override fun update(eventId: Long, event: Event) {
        val updated = dsl.execute(
            """
            UPDATE event
            SET name = ?,
                description = ?,
                start_at = CAST(? AS timestamptz),
                end_at = CAST(? AS timestamptz),
                location_id = ?,
                user_id = ?
            WHERE id = ?
            """.trimIndent(),
            event.name,
            event.description,
            event.startsAt.toOffsetDateTimeUtc(),
            event.endsAt?.toOffsetDateTimeUtc(),
            event.locationId,
            event.userId,
            eventId
        )
        if (updated == 0) throw EventNotFoundException(eventId)
        replaceEventCategories(eventId, event.categoryIds)
    }

    @Transactional
    override fun delete(eventId: Long) {
        ensureEventExists(eventId)
        val photoPaths = dsl.fetch(
            """
            SELECT image_path
            FROM photo
            WHERE event_id = ?
            """.trimIndent(),
            eventId
        ).mapNotNull { it.get("image_path", String::class.java) }

        dsl.execute("DELETE FROM decision WHERE report_id IN (SELECT id FROM report WHERE event_id = ?)", eventId)
        dsl.execute("DELETE FROM report WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM advertising WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM repost WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM rating WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM comment WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM category_event WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM event_list WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM photo WHERE event_id = ?", eventId)
        dsl.execute("DELETE FROM event WHERE id = ?", eventId)
        fileStorageService.deleteAllByStoredPath(photoPaths)
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
                else -> BigDecimal("3.0")
            }

        return score.coerceIn(BigDecimal.ZERO, BigDecimal("5.0")).setScale(1, RoundingMode.HALF_UP)
    }

    private fun BigDecimal.coerceIn(min: BigDecimal, max: BigDecimal): BigDecimal =
        when {
            this < min -> min
            this > max -> max
            else -> this
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
