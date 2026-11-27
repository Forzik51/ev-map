package com.evmap.serverapp.features.event.infra.write

import com.evmap.serverapp.features.event.api.dto.ViewEventPhoto
import com.evmap.serverapp.features.event.application.EventNotFoundException
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class EventPhotoRepository(
    private val dsl: DSLContext,
) {
    fun lockEventOwner(eventId: Long): Long =
        dsl.fetchOne("SELECT user_id FROM event WHERE id = ? FOR UPDATE", eventId)?.get(0, Long::class.java)
            ?: throw EventNotFoundException(eventId)

    fun findByUploadId(eventId: Long, uploadId: UUID): ViewEventPhoto? =
        dsl.fetchOne(
            "SELECT id, event_id, image_path FROM photo WHERE event_id = ? AND image_path LIKE ?",
            eventId,
            "/media/events/$eventId/$uploadId-%.%",
        )?.let(::toView)

    fun create(eventId: Long, imagePath: String): ViewEventPhoto {
        ensureEventExists(eventId)
        val record = dsl.fetchOne(
            """
            INSERT INTO photo(event_id, image_path, sort_order)
            SELECT ?, ?, COALESCE(MAX(sort_order),-1)+1 FROM photo WHERE event_id=?
            RETURNING id, event_id, image_path
            """.trimIndent(),
            eventId,
            imagePath, eventId
        ) ?: error("Failed to save event photo")

        return toView(record)
    }

    fun findAllByEventId(eventId: Long): List<ViewEventPhoto> {
        ensureEventExists(eventId)
        return dsl.fetch(
            """
            SELECT id, event_id, image_path
            FROM photo
            WHERE event_id = ?
            ORDER BY sort_order,id
            """.trimIndent(),
            eventId
        ).map { toView(it) }
    }

    fun findById(eventId: Long, photoId: Long): ViewEventPhoto? {
        ensureEventExists(eventId)
        return dsl.fetchOne(
            """
            SELECT id, event_id, image_path
            FROM photo
            WHERE event_id = ? AND id = ?
            """.trimIndent(),
            eventId,
            photoId
        )?.let { toView(it) }
    }

    fun updatePath(eventId: Long, photoId: Long, imagePath: String): ViewEventPhoto? {
        ensureEventExists(eventId)
        return dsl.fetchOne(
            """
            UPDATE photo
            SET image_path = ?
            WHERE event_id = ? AND id = ?
            RETURNING id, event_id, image_path
            """.trimIndent(),
            imagePath,
            eventId,
            photoId
        )?.let { toView(it) }
    }

    fun delete(eventId: Long, photoId: Long): String? {
        ensureEventExists(eventId)
        return dsl.fetchOne(
            """
            DELETE FROM photo
            WHERE event_id = ? AND id = ?
            RETURNING image_path
            """.trimIndent(),
            eventId,
            photoId
        )?.get("image_path", String::class.java)
    }

    fun findPathsByEventId(eventId: Long): List<String> =
        dsl.fetch(
            """
            SELECT image_path
            FROM photo
            WHERE event_id = ?
            """.trimIndent(),
            eventId
        ).mapNotNull { it.get("image_path", String::class.java) }

    private fun ensureEventExists(eventId: Long) {
        val exists = dsl.fetchOne("SELECT 1 FROM event WHERE id = ?", eventId) != null
        if (!exists) throw EventNotFoundException(eventId)
    }

    fun order(eventId: Long, ids: List<Long>) {
        ids.forEachIndexed { index,id -> dsl.execute("UPDATE photo SET sort_order=? WHERE event_id=? AND id=?",index,eventId,id) }
    }

    private fun toView(record: org.jooq.Record): ViewEventPhoto =
        ViewEventPhoto(
            id = record.get("id", Long::class.java)!!,
            eventId = record.get("event_id", Long::class.java)!!,
            imagePath = record.get("image_path", String::class.java)!!,
        )
}
