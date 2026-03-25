package com.evmap.serverapp.features.location.infra.read

import com.evmap.serverapp.features.location.api.dto.ViewLocationCategory
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class LocationReadRepository(
    private val dsl: DSLContext,
) {
    fun findAllCategories(): List<ViewLocationCategory> =
        dsl.fetch(
            """
            SELECT id, name
            FROM location_category
            ORDER BY id
            """.trimIndent()
        ).map {
            ViewLocationCategory(
                id = it.get("id", Long::class.java)!!,
                name = it.get("name", String::class.java)!!,
            )
        }
}
