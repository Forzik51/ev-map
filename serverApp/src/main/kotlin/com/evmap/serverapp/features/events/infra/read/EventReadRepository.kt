package com.evmap.serverapp.features.events.infra.read

import com.evmap.serverapp.features.events.api.dto.EventView
import org.jooq.DSLContext
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Repository
import org.springframework.web.server.ResponseStatusException
import java.sql.Timestamp

@Repository
class EventReadRepository(private val dsl: DSLContext) {

    fun findViewById(id: Long): EventView =
        dsl.fetchOne(
            """
            SELECT w.id, w.nazwa, w.opis, w.kiedy, ST_AsText(l.loklizacja_punkt) AS location_name
            FROM Wydarzenie w
            LEFT JOIN lokalizacja l ON l.id = w.Lokalizacja_id
            WHERE w.id = 1
            """.trimIndent(), id
        )?.let { r -> val kiedy = r.get("kiedy", Timestamp::class.java)
            ?: throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "kiedy is null for id=$id")

            EventView(
                id = r.get("id", Long::class.java),
                name = r.get("nazwa", String::class.java),
                description = r.get("opis", String::class.java),
                startsAt = kiedy.toInstant().toString(),
                locationName = r.get("location_name", String::class.java) ?: ""
            )
        } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Event $id not found")


    fun findAllViews(pageable: Pageable): Page<EventView> {

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
            EventView(
                id = it.get("id", Long::class.java),
                name = it.get("nazwa", String::class.java),
                description = it.get("opis", String::class.java),
                startsAt = it.get("kiedy", Timestamp::class.java).toInstant().toString(),
                locationName = it.get("nazwa", String::class.java)
            )
        }

        // liczba rekordów do Page<>
        val total = dsl.fetchOne(
            """
            SELECT COUNT(*) 
            FROM wydarzenie w
            LEFT JOIN Lokalizacja l ON l.id = w.Lokalizacja_id
            LEFT JOIN Kategoria_lokalizacji k ON k.id = l.Kategoria_lokalizacji_id
            """.trimIndent()
        )?.get(0).toString().toLong()

        return PageImpl(items, pageable, total)
    }
}