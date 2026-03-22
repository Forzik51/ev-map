package com.evmap.serverapp.features.event.infra.read

import com.evmap.serverapp.features.event.api.dto.ViewEvent
import com.evmap.serverapp.features.event.application.EventNotFoundException
import org.jooq.DSLContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
class EventReadRepository(private val dsl: DSLContext) {

    fun findViewById(id: Long): ViewEvent =
        dsl.fetchOne(
            """
            SELECT w.id, w.nazwa, w.opis, w.kiedy, ST_AsText(l.loklizacja_punkt) AS location_name
            FROM Wydarzenie w
            LEFT JOIN lokalizacja l ON l.id = w.Lokalizacja_id
            WHERE w.id = ?
            """.trimIndent(),
            id
        )?.let { r ->
            val whenTs = r.get("kiedy", Timestamp::class.java)
                ?: throw IllegalStateException("kiedy is null for id=$id")

            ViewEvent(
                id = requireNotNull(r.get("id", Long::class.java)) { "id is null for id=$id" },
                name = requireNotNull(r.get("nazwa", String::class.java)) { "nazwa is null for id=$id" },
                description = requireNotNull(r.get("opis", String::class.java)) { "opis is null for id=$id" },
                startsAt = whenTs.toInstant().toString(),
                locationName = r.get("location_name", String::class.java)
            )
        } ?: throw EventNotFoundException(id)

    fun findAllViews(pageable: Pageable): Page<ViewEvent> {
        val sql = """
            SELECT w.id, w.nazwa, w.opis, w.kiedy, k.nazwa AS location_name
            FROM wydarzenie w
            LEFT JOIN Lokalizacja l ON l.id = w.Lokalizacja_id
            LEFT JOIN Kategoria_lokalizacji k ON k.id = l.Kategoria_lokalizacji_id
            ORDER BY w.kiedy DESC
            LIMIT ? OFFSET ?
        """.trimIndent()

        val recs = dsl.resultQuery(sql, pageable.pageSize, pageable.offset).fetch()

        val items = recs.map {
            ViewEvent(
                id = requireNotNull(it.get("id", Long::class.java)) { "id is null for event row" },
                name = requireNotNull(it.get("nazwa", String::class.java)) { "nazwa is null for event row" },
                description = requireNotNull(it.get("opis", String::class.java)) { "opis is null for event row" },
                startsAt = requireNotNull(it.get("kiedy", Timestamp::class.java)) { "kiedy is null for event row" }
                    .toInstant().toString(),
                locationName = it.get("location_name", String::class.java)
            )
        }

        val total = dsl.fetchOne(
            """
            SELECT COUNT(*)
            FROM wydarzenie w
            LEFT JOIN Lokalizacja l ON l.id = w.Lokalizacja_id
            LEFT JOIN Kategoria_lokalizacji k ON k.id = l.Kategoria_lokalizacji_id
            """.trimIndent()
        )?.get(0, Long::class.java) ?: 0L

        return PageImpl(items, pageable, total)
    }
}
