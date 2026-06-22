package com.evmap.mobileapp.feature.reports.data

import com.evmap.mobileapp.core.data.dto.CreateReportRequest
import com.evmap.mobileapp.core.data.dto.ReportCategoryDto
import com.evmap.mobileapp.core.data.dto.ReportDto
import com.evmap.mobileapp.core.data.dto.SendEmailRequest
import com.evmap.mobileapp.core.data.remote.NotificationsApi
import com.evmap.mobileapp.core.data.remote.ReportsApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportsRepository @Inject constructor(
    private val reportsApi: ReportsApi,
    private val notificationsApi: NotificationsApi,
) {
    suspend fun categories(): Result<List<ReportCategoryDto>> = runCatching {
        reportsApi.categories()
    }

    suspend fun create(eventId: Long, categoryId: Long, description: String): Result<Long> = runCatching {
        reportsApi.create(
            CreateReportRequest(
                eventId = eventId,
                categoryId = categoryId,
                description = description,
            )
        )
    }

    suspend fun all(): Result<List<ReportDto>> = runCatching {
        reportsApi.all()
    }

    suspend fun updateStatus(reportId: Long, status: String): Result<Unit> = runCatching {
        reportsApi.updateStatus(reportId, status)
        Unit
    }

    suspend fun delete(reportId: Long): Result<Unit> = runCatching {
        reportsApi.delete(reportId)
        Unit
    }

    suspend fun sendEmailToAll(subject: String, body: String): Result<Unit> = runCatching {
        notificationsApi.sendToAll(SendEmailRequest(subject, body))
        Unit
    }

    suspend fun sendEmailToUser(userId: Long, subject: String, body: String): Result<Unit> = runCatching {
        notificationsApi.sendToUser(userId, SendEmailRequest(subject, body))
        Unit
    }
}
