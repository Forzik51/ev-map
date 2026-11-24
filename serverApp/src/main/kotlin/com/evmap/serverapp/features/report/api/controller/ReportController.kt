package com.evmap.serverapp.features.report.api.controller

import com.evmap.serverapp.features.report.api.dto.CreateReport
import com.evmap.serverapp.features.report.api.dto.DecideReportRequest
import com.evmap.serverapp.features.report.api.dto.ViewReportCategory
import com.evmap.serverapp.features.report.api.dto.ViewReport
import com.evmap.serverapp.features.report.api.dto.ViewReportDetails
import com.evmap.serverapp.features.report.application.command.CreateReport as CreateReportCommand
import com.evmap.serverapp.features.report.application.command.DecideReport
import com.evmap.serverapp.features.report.application.command.DeleteReport
import com.evmap.serverapp.features.report.application.query.GetAllReportCategories
import com.evmap.serverapp.features.report.application.query.GetAllReports
import com.evmap.serverapp.features.report.application.query.GetReportDetails
import com.evmap.serverapp.features.report.domian.ReportStatus
import com.evmap.serverapp.shared.security.CurrentUserProvider
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val createReport: CreateReportCommand,
    private val deleteReport: DeleteReport,
    private val decideReport: DecideReport,
    private val getAllReportCategories: GetAllReportCategories,
    private val getAllReports: GetAllReports,
    private val getReportDetails: GetReportDetails,
    private val currentUserProvider: CurrentUserProvider,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody dto: CreateReport): Long =
        createReport.handle(dto, currentUserProvider.get().id)

    @GetMapping("/categories")
    fun getAllCategories(): List<ViewReportCategory> = getAllReportCategories.handle()

    @GetMapping
    fun getAll(@RequestParam(required = false) status: ReportStatus?): List<ViewReport> {
        currentUserProvider.requireAdmin()
        return getAllReports.handle(status)
    }

    @GetMapping("/{reportId}")
    fun getDetails(@PathVariable reportId: Long): ViewReportDetails {
        currentUserProvider.requireAdmin()
        return getReportDetails.handle(reportId)
    }

    @PostMapping("/{reportId}/decision")
    fun decide(
        @PathVariable reportId: Long,
        @Valid @RequestBody request: DecideReportRequest,
    ): ViewReportDetails {
        currentUserProvider.requireAdmin()
        return decideReport.handle(reportId, currentUserProvider.get().id, request)
    }

    @PatchMapping("/{reportId}/status")
    fun legacyStatus(@PathVariable reportId: Long) {
        currentUserProvider.requireAdmin()
        throw ResponseStatusException(HttpStatus.GONE, "Use POST /api/reports/$reportId/decision")
    }

    @DeleteMapping("/{reportId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable reportId: Long) {
        currentUserProvider.requireAdmin()
        deleteReport.handle(reportId)
    }
}
