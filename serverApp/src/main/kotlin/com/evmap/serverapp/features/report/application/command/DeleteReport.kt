package com.evmap.serverapp.features.report.application.command

import com.evmap.serverapp.features.report.domian.ReportRepositoryPort
import org.springframework.stereotype.Service

@Service
class DeleteReport(
    private val repo: ReportRepositoryPort,
) {
    fun handle(reportId: Long) {
        repo.delete(reportId)
    }
}
