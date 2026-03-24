package com.evmap.serverapp.features.report.domian

data class Report(
    val id: Long? = null,
    val description: String,
    val eventId: Long,
    val categoryId: Long,
    val userId: Long,
)
