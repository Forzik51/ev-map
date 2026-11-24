package com.evmap.mobileapp.feature.reports.data

import com.evmap.mobileapp.core.data.dto.CreateReportRequest
import com.evmap.mobileapp.core.data.dto.DecideReportRequest
import com.evmap.mobileapp.core.data.dto.EmailDeliverySummary
import com.evmap.mobileapp.core.data.dto.ReportCategoryDto
import com.evmap.mobileapp.core.data.dto.ReportDetailsDto
import com.evmap.mobileapp.core.data.dto.ReportDto
import com.evmap.mobileapp.core.data.dto.ReportStatus
import com.evmap.mobileapp.core.data.dto.SendEmailRequest
import com.evmap.mobileapp.core.data.remote.NotificationsApi
import com.evmap.mobileapp.core.data.remote.ReportsApi
import com.evmap.mobileapp.core.data.remote.toUserMessage
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportsRepository @Inject constructor(
    private val reportsApi: ReportsApi,
    private val notificationsApi: NotificationsApi,
) {
    suspend fun categories(): Result<List<ReportCategoryDto>> = request {
        reportsApi.categories()
    }

    suspend fun create(eventId: Long, categoryId: Long, description: String): Result<Long> = request {
        reportsApi.create(
            CreateReportRequest(
                eventId = eventId,
                categoryId = categoryId,
                description = description,
            )
        )
    }

    suspend fun all(status: ReportStatus): Result<List<ReportDto>> = request {
        reportsApi.all(status)
    }

    suspend fun details(reportId: Long): Result<ReportDetailsDto> = request {
        reportsApi.details(reportId)
    }

    suspend fun decide(reportId: Long, status: ReportStatus, reason: String): Result<ReportDetailsDto> = request {
        reportsApi.decide(reportId, DecideReportRequest(status, reason))
    }

    suspend fun delete(reportId: Long): Result<Unit> = request {
        reportsApi.delete(reportId).requireSuccess()
    }

    suspend fun sendEmailToAll(subject: String, body: String): Result<EmailDeliverySummary> = request {
        notificationsApi.sendToAll(SendEmailRequest(subject, body))
    }

    suspend fun sendEmailToUser(userId: Long, subject: String, body: String): Result<Unit> = request {
        notificationsApi.sendToUser(userId, SendEmailRequest(subject, body)).requireSuccess()
    }

    private fun Response<Unit>.requireSuccess() {
        if (!isSuccessful) throw HttpException(this)
    }

    private suspend fun <T> request(call: suspend () -> T): Result<T> = try {
        Result.success(call())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}

internal fun Throwable.reportMessage(): String = when ((this as? HttpException)?.code() ?: 0) {
    400 -> "Check the entered information and try again."
    401 -> "Sign in again to continue."
    403 -> "You do not have access to this action."
    404 -> "The report or event is no longer available."
    in 500..599 -> "The server could not complete the request. Try again."
    else -> toUserMessage()
}
