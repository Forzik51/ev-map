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
    val categoryId: Long,
    val status: String,
    val userId: Long,
)

data class ReportCategoryDto(
    val id: Long,
    val name: String,
)

data class SendEmailRequest(
    val subject: String,
    val body: String,
)
