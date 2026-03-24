package com.evmap.serverapp.features.report.application.command

import com.evmap.serverapp.features.report.domian.ReportRepositoryPort
import org.springframework.stereotype.Service

@Service
class EditReportStatus(
    private val repo: ReportRepositoryPort,
) {
    fun handle(reportId: Long, status: String) {
        repo.updateStatus(reportId, status)
    }
}
