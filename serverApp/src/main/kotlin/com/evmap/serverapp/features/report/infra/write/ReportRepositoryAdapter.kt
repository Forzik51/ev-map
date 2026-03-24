package com.evmap.serverapp.features.report.infra.write

import com.evmap.serverapp.features.report.application.ReportNotFoundException
import com.evmap.serverapp.features.report.domian.Report
import com.evmap.serverapp.features.report.domian.ReportRepositoryPort
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
            VALUES (?, ?, ?, ?)
            RETURNING id
            """.trimIndent(),
            report.description,
            report.eventId,
            report.categoryId,
            report.userId
        )?.get("id", Long::class.java) ?: error("Failed to create report")

    override fun delete(reportId: Long) {
        val deleted = dsl.execute("DELETE FROM report WHERE id = ?", reportId)
        if (deleted == 0) throw ReportNotFoundException(reportId)
    }

    @Transactional
    override fun updateStatus(reportId: Long, status: String) {
        val categoryId = dsl.fetchOne(
            "SELECT id FROM report_category WHERE lower(report_type) = lower(?)",
            status
        )?.get("id", Long::class.java)
            ?: dsl.fetchOne(
                """
                INSERT INTO report_category(report_type)
                VALUES (?)
                RETURNING id
                """.trimIndent(),
                status
            )?.get("id", Long::class.java)
            ?: error("Failed to resolve report category")

        val updated = dsl.execute(
            "UPDATE report SET category_id = ? WHERE id = ?",
            categoryId,
            reportId
        )
        if (updated == 0) throw ReportNotFoundException(reportId)
    }
}
