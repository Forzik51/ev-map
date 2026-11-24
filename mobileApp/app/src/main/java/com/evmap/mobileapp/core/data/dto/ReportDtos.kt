package com.evmap.mobileapp.core.data.dto

data class CreateReportRequest(
    val description: String,
    val eventId: Long,
    val categoryId: Long,
)

data class ReportDto(
    val id: Long,
    val description: String,
    val eventId: Long,
    val eventName: String,
    val categoryId: Long,
    val categoryName: String,
    val status: ReportStatus,
    val userId: Long,
)

enum class ReportStatus { OPEN, RESOLVED, DISMISSED }

data class DecideReportRequest(val status: ReportStatus, val reason: String)

data class ReportDetailsDto(
    val report: ReportDto,
    val decisions: List<ReportDecisionDto>,
)

data class ReportDecisionDto(
    val id: Long,
    val reason: String,
    val outcome: ReportStatus?,
    val decidedAt: String?,
    val moderatorId: Long?,
    val moderatorUsername: String?,
    val blockedUntil: String?,
)

data class ReportCategoryDto(
    val id: Long,
    val name: String,
)

data class SendEmailRequest(
    val subject: String,
    val body: String,
)
