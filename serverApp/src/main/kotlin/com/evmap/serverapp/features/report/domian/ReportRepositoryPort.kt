package com.evmap.serverapp.features.report.domian

interface ReportRepositoryPort {
    fun save(report: Report): Long
    fun delete(reportId: Long)
    fun decide(reportId: Long, moderatorId: Long, status: ReportStatus, reason: String)
}
