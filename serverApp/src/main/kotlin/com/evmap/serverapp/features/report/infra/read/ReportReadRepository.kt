package com.evmap.serverapp.features.report.infra.read

import com.evmap.serverapp.features.report.api.dto.ViewReport
import com.evmap.serverapp.features.report.api.dto.ViewReportCategory
import com.evmap.serverapp.features.report.api.dto.ViewReportDecision
import com.evmap.serverapp.features.report.api.dto.ViewReportDetails
import com.evmap.serverapp.features.report.domian.ReportStatus
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class ReportReadRepository(
    private val dsl: DSLContext
) {
    fun findAllCategories(): List<ViewReportCategory> =
        dsl.fetch(
            """
            SELECT id, report_type
            FROM report_category
            WHERE is_selectable
            ORDER BY id
            """.trimIndent()
        ).map {
            ViewReportCategory(
                id = it.get("id", Long::class.java)!!,
                name = it.get("report_type", String::class.java)!!,
            )
        }

    fun findAll(status: ReportStatus?): List<ViewReport> {
        val reports = if (status == null) {
            dsl.fetch("SELECT $reportFields $reportTables ORDER BY r.id DESC")
        } else {
            dsl.fetch("SELECT $reportFields $reportTables WHERE r.status = ? ORDER BY r.id DESC", status.name)
        }
        return reports.map { it.toViewReport() }
    }

    fun findDetails(reportId: Long): ViewReportDetails? {
        // Read the status and decision history from the same PostgreSQL snapshot.
        val rows = dsl.fetch(
            """
            SELECT $reportFields,
                   d.id AS decision_id, d.description AS reason, d.outcome, d.decided_at, d.moderator_id,
                   u.username AS moderator_username, d.blocked_until
            $reportTables
            LEFT JOIN decision d ON d.report_id = r.id
            LEFT JOIN app_user u ON u.id = d.moderator_id
            WHERE r.id = ?
            ORDER BY d.id DESC
            """.trimIndent(),
            reportId,
        )
        val report = rows.firstOrNull()?.toViewReport() ?: return null
        val decisions = rows.mapNotNull { it.toViewDecision() }
        return ViewReportDetails(report, decisions)
    }

    private fun Record.toViewReport() = ViewReport(
        id = get("id", Long::class.javaObjectType)!!,
        description = get("description", String::class.java)!!,
        eventId = get("event_id", Long::class.javaObjectType)!!,
        eventName = get("event_name", String::class.java)!!,
        categoryId = get("category_id", Long::class.javaObjectType)!!,
        categoryName = get("category_name", String::class.java)!!,
        status = ReportStatus.valueOf(get("status", String::class.java)!!),
        userId = get("user_id", Long::class.javaObjectType)!!,
    )

    private fun Record.toViewDecision(): ViewReportDecision? {
        val id = get("decision_id", Long::class.javaObjectType) ?: return null
        return ViewReportDecision(
            id = id,
            reason = get("reason", String::class.java)!!,
            outcome = get("outcome", String::class.java)?.let(ReportStatus::valueOf),
            decidedAt = get("decided_at", OffsetDateTime::class.java),
            moderatorId = get("moderator_id", Long::class.javaObjectType),
            moderatorUsername = get("moderator_username", String::class.java),
            blockedUntil = get("blocked_until", OffsetDateTime::class.java),
        )
    }

    private companion object {
        val reportFields = """
            r.id, r.description, r.event_id, e.name AS event_name, r.category_id, c.report_type AS category_name,
            r.status, r.user_id
        """.trimIndent()
        val reportTables = """
            FROM report r
            JOIN report_category c ON c.id = r.category_id
            JOIN event e ON e.id = r.event_id
        """.trimIndent()
    }
}
