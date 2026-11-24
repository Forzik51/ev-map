package com.evmap.serverapp.features.report.application.command

import com.evmap.serverapp.features.report.api.dto.CreateReport as CreateReportRequest
import com.evmap.serverapp.features.report.domian.Report
import com.evmap.serverapp.features.report.domian.ReportRepositoryPort
import org.springframework.stereotype.Service

@Service
class CreateReport(private val repo: ReportRepositoryPort) {
    fun handle(dto: CreateReportRequest, userId: Long): Long =
        repo.save(
            Report(
                description = dto.description.trim(),
                eventId = dto.eventId,
                categoryId = dto.categoryId,
                userId = userId,
            )
        )
}
