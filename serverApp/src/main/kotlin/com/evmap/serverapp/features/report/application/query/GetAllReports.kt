package com.evmap.serverapp.features.report.application.query

import com.evmap.serverapp.features.report.api.dto.ViewReport
import com.evmap.serverapp.features.report.infra.read.ReportReadRepository
import org.springframework.stereotype.Service

@Service
class GetAllReports(
    private val readRepo: ReportReadRepository,
) {
    fun handle(): List<ViewReport> = readRepo.findAll()
}
