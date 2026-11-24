package com.evmap.serverapp.features.report.api.dto

import com.evmap.serverapp.features.report.domian.ReportStatus
import jakarta.validation.constraints.NotBlank

data class DecideReportRequest(
    val status: ReportStatus,
    @field:NotBlank val reason: String,
)
