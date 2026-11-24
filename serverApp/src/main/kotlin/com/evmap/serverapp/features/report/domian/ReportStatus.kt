package com.evmap.serverapp.features.report.domian

import com.fasterxml.jackson.annotation.JsonCreator

enum class ReportStatus {
    OPEN,
    RESOLVED,
    DISMISSED;

    companion object {
        @JvmStatic
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        fun fromJson(value: String): ReportStatus = valueOf(value)
    }
}
