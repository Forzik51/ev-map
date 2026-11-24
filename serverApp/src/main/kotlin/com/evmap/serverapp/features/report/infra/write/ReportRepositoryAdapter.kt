package com.evmap.serverapp.features.report.infra.write

import com.evmap.serverapp.features.report.application.InvalidReportException
import com.evmap.serverapp.features.report.application.ReportConflictException
import com.evmap.serverapp.features.report.application.ReportNotFoundException
import com.evmap.serverapp.features.report.domian.Report
import com.evmap.serverapp.features.report.domian.ReportRepositoryPort
import com.evmap.serverapp.features.report.domian.ReportStatus
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class ReportRepositoryAdapter(
    private val dsl: DSLContext
) : ReportRepositoryPort {

    override fun save(report: Report): Long =
        dsl.fetchOne(
            """
            INSERT INTO report(description, event_id, category_id, user_id)
            SELECT ?, ?, c.id, ?
            FROM report_category c
            WHERE c.id = ? AND c.is_selectable
            RETURNING id
            """.trimIndent(),
            report.description,
            report.eventId,
            report.userId,
            report.categoryId,
        )?.get("id", Long::class.javaObjectType) ?: throw InvalidReportException("Choose an available report category.")

    @Transactional
    override fun delete(reportId: Long) {
        val status = lockReport(reportId)
        if (status != ReportStatus.OPEN || dsl.fetchOne("SELECT 1 FROM decision WHERE report_id = ? LIMIT 1", reportId) != null) {
            throw ReportConflictException("Only open reports without decisions can be deleted.")
        }
        dsl.execute("DELETE FROM report WHERE id = ?", reportId)
    }

    @Transactional
    override fun decide(reportId: Long, moderatorId: Long, status: ReportStatus, reason: String) {
        // Lock the moderator first, as account deletion locks the account before its reports.
        if (dsl.fetchOne("SELECT id FROM app_user WHERE id = ? FOR KEY SHARE", moderatorId) == null) {
            throw ReportConflictException("The moderator account is no longer available.")
        }
        val current = lockReport(reportId)
        if (current != ReportStatus.OPEN) {
            val alreadySaved = current == status && dsl.fetchOne(
                """
                SELECT id FROM decision
                WHERE report_id = ? AND moderator_id = ? AND outcome = ? AND description = ?
                """.trimIndent(),
                reportId, moderatorId, status.name, reason,
            ) != null
            if (alreadySaved) return
            throw ReportConflictException("This report has already been reviewed.")
        }

        dsl.execute(
            """
            INSERT INTO decision(report_id, moderator_id, outcome, description, decided_at)
            VALUES (?, ?, ?, ?, clock_timestamp())
            """.trimIndent(),
            reportId, moderatorId, status.name, reason,
        )
        dsl.execute("UPDATE report SET status = ? WHERE id = ?", status.name, reportId)
    }

    private fun lockReport(reportId: Long): ReportStatus =
        dsl.fetchOne("SELECT status FROM report WHERE id = ? FOR UPDATE", reportId)
            ?.get("status", String::class.java)
            ?.let(ReportStatus::valueOf)
            ?: throw ReportNotFoundException(reportId)
}
