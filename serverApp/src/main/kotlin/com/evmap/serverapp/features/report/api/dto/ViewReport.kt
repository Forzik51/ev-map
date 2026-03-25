package com.evmap.serverapp.features.report.api.dto

data class ViewReport(
    val id: Long,
    val description: String,
    val eventId: Long,
    val categoryId: Long,
    val status: String,
    val userId: Long,
)
