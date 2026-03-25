package com.evmap.serverapp.features.report.application.query

import com.evmap.serverapp.features.report.api.dto.ViewReportCategory
import com.evmap.serverapp.features.report.infra.read.ReportReadRepository
import org.springframework.stereotype.Service

@Service
class GetAllReportCategories(
    private val readRepo: ReportReadRepository,
) {
    fun handle(): List<ViewReportCategory> = readRepo.findAllCategories()
}
