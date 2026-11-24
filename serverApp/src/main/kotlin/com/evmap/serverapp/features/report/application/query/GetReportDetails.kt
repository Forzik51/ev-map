package com.evmap.serverapp.features.report.application.query

import com.evmap.serverapp.features.report.api.dto.ViewReportDetails
import com.evmap.serverapp.features.report.application.ReportNotFoundException
import com.evmap.serverapp.features.report.infra.read.ReportReadRepository
import org.springframework.stereotype.Service

@Service
class GetReportDetails(private val readRepo: ReportReadRepository) {
    fun handle(reportId: Long): ViewReportDetails =
        readRepo.findDetails(reportId) ?: throw ReportNotFoundException(reportId)
}
