package com.evmap.serverapp.features.report.api.dto

import com.evmap.serverapp.features.report.domian.ReportStatus
import java.time.OffsetDateTime

data class ViewReport(
    val id: Long,
    val description: String,
    val eventId: Long,
    val eventName: String,
    val categoryId: Long,
    val categoryName: String,
    val status: ReportStatus,
    val userId: Long,
)

data class ViewReportDetails(
    val report: ViewReport,
    val decisions: List<ViewReportDecision>,
)

data class ViewReportDecision(
    val id: Long,
    val reason: String,
    val outcome: ReportStatus?,
    val decidedAt: OffsetDateTime?,
    val moderatorId: Long?,
    val moderatorUsername: String?,
    val blockedUntil: OffsetDateTime?,
)
