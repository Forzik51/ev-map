package com.evmap.serverapp.features.report.domian

interface ReportRepositoryPort {
    fun save(report: Report): Long
    fun delete(reportId: Long)
    fun updateStatus(reportId: Long, status: String)
}
