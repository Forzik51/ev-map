package com.evmap.serverapp.features.report.infra.read

import com.evmap.serverapp.features.report.api.dto.ViewReport
import com.evmap.serverapp.features.report.api.dto.ViewReportCategory
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ReportReadRepository(
    private val dsl: DSLContext
) {
    fun findAllCategories(): List<ViewReportCategory> =
        dsl.fetch(
            """
            SELECT id, report_type
            FROM report_category
            ORDER BY id
            """.trimIndent()
        ).map {
            ViewReportCategory(
                id = it.get("id", Long::class.java)!!,
                name = it.get("report_type", String::class.java)!!,
            )
        }

    fun findAll(): List<ViewReport> =
        dsl.fetch(
            """
            SELECT r.id,
                   r.description,
                   r.event_id,
                   r.category_id,
                   rc.report_type AS status,
                   r.user_id
            FROM report r
            JOIN report_category rc ON rc.id = r.category_id
            ORDER BY r.id DESC
            """.trimIndent()
        ).map {
            ViewReport(
                id = it.get("id", Long::class.java)!!,
                description = it.get("description", String::class.java)!!,
                eventId = it.get("event_id", Long::class.java)!!,
                categoryId = it.get("category_id", Long::class.java)!!,
                status = it.get("status", String::class.java)!!,
                userId = it.get("user_id", Long::class.java)!!,
            )
        }
}
