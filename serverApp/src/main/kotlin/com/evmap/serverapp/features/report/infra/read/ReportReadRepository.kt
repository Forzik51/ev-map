package com.evmap.serverapp.features.report.infra.read

import com.evmap.serverapp.features.report.api.dto.ViewReport
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ReportReadRepository(
    private val dsl: DSLContext
) {
    fun findAll(): List<ViewReport> =
        dsl.fetch(
            """
            SELECT r.id,
                   r.description,
                   r.event_id,
                   r.category_id,
                   rc.report_type,
                   r.user_id,
                   u.username
            FROM report r
            JOIN report_category rc ON rc.id = r.category_id
            JOIN app_user u ON u.id = r.user_id
            ORDER BY r.id DESC
            """.trimIndent()
        ).map {
            ViewReport(
                id = it.get("id", Long::class.java)!!,
                description = it.get("description", String::class.java)!!,
                eventId = it.get("event_id", Long::class.java)!!,
                categoryId = it.get("category_id", Long::class.java)!!,
                categoryType = it.get("report_type", String::class.java)!!,
                userId = it.get("user_id", Long::class.java)!!,
                username = it.get("username", String::class.java)!!,
            )
        }
}
