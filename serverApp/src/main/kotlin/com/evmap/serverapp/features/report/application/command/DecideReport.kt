package com.evmap.serverapp.features.report.application.command

import com.evmap.serverapp.features.report.api.dto.DecideReportRequest
import com.evmap.serverapp.features.report.api.dto.ViewReportDetails
import com.evmap.serverapp.features.report.application.InvalidReportException
import com.evmap.serverapp.features.report.application.query.GetReportDetails
import com.evmap.serverapp.features.report.domian.ReportRepositoryPort
import com.evmap.serverapp.features.report.domian.ReportStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DecideReport(
    private val repository: ReportRepositoryPort,
    private val getReportDetails: GetReportDetails,
) {
    @Transactional
    fun handle(reportId: Long, moderatorId: Long, request: DecideReportRequest): ViewReportDetails {
        if (request.status == ReportStatus.OPEN) {
            throw InvalidReportException("Choose resolved or dismissed.")
        }
        val reason = request.reason.trim()
        if (reason.length !in 1..500) {
            throw InvalidReportException("Enter a reason of up to 500 characters.")
        }
        repository.decide(reportId, moderatorId, request.status, reason)
        return getReportDetails.handle(reportId)
    }
}
