package com.evmap.serverapp.features.events.infra.read

import com.evmap.serverapp.features.events.api.dto.EventView
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
class EventReadRepository(private val dsl: DSLContext) {

    fun findViewById(id: Long): EventView =
        dsl.fetchOne(
            """
            SELECT w.id, w.nazwa, w.opis, w.kiedy, l.loklizacja_punkt AS location_name
            FROM Wydarzenie w
            LEFT JOIN "Lokalizacja" l ON l.id = w.Lokalizacja_id
            WHERE w.id = ?
            """.trimIndent(), id
        )!!.let {
            EventView(
                id = it.get("id", Long::class.java),
                name = it.get("nazwa", String::class.java),
                description = it.get("opis", String::class.java),
                startsAt = it.get("kiedy", Timestamp::class.java).toInstant().toString(),
                locationName = it.get("location_name", String::class.java)
            )
        }

    fun findAllViews(): List<EventView> =
        dsl.fetch(
            """
            SELECT w.id, w.nazwa, w.opis, w.kiedy, k.nazwa AS location_name
            FROM wydarzenie w
            LEFT JOIN Lokalizacja l ON l.id = w.Lokalizacja_id
			LEFT JOIN Kategoria_lokalizacji k ON k.id = l.id
            ORDER BY w.kiedy DESC
            """.trimIndent()
        ).map {
            EventView(
                id = it.get("id", Long::class.java),
                name = it.get("nazwa", String::class.java),
                description = it.get("opis", String::class.java),
                startsAt = it.get("kiedy", Timestamp::class.java).toInstant().toString(),
                locationName = it.get("nazwa", String::class.java)
            )
        }
}