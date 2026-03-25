package com.evmap.serverapp.features.report.api.controller

import com.evmap.serverapp.features.report.api.dto.CreateReport
import com.evmap.serverapp.features.report.api.dto.ViewReportCategory
import com.evmap.serverapp.features.report.api.dto.ViewReport
import com.evmap.serverapp.features.report.application.command.CreateReport as CreateReportCommand
import com.evmap.serverapp.features.report.application.command.DeleteReport
import com.evmap.serverapp.features.report.application.command.EditReportStatus
import com.evmap.serverapp.features.report.application.query.GetAllReportCategories
import com.evmap.serverapp.features.report.application.query.GetAllReports
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

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val createReport: CreateReportCommand,
    private val deleteReport: DeleteReport,
    private val editReportStatus: EditReportStatus,
    private val getAllReportCategories: GetAllReportCategories,
    private val getAllReports: GetAllReports,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody dto: CreateReport): Long = createReport.handle(dto)

    @GetMapping("/categories")
    fun getAllCategories(): List<ViewReportCategory> = getAllReportCategories.handle()

    @GetMapping
    fun getAll(): List<ViewReport> = getAllReports.handle()

    @PatchMapping("/{reportId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateStatus(@PathVariable reportId: Long, @RequestParam status: String) {
        editReportStatus.handle(reportId, status)
    }

    @DeleteMapping("/{reportId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable reportId: Long) {
        deleteReport.handle(reportId)
    }
}
