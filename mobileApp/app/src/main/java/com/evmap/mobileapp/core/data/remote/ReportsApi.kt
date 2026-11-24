package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.CreateReportRequest
import com.evmap.mobileapp.core.data.dto.DecideReportRequest
import com.evmap.mobileapp.core.data.dto.ReportCategoryDto
import com.evmap.mobileapp.core.data.dto.ReportDetailsDto
import com.evmap.mobileapp.core.data.dto.ReportDto
import com.evmap.mobileapp.core.data.dto.ReportStatus
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReportsApi {
    @POST("api/reports")
    suspend fun create(@Body request: CreateReportRequest): Long

    @GET("api/reports/categories")
    suspend fun categories(): List<ReportCategoryDto>

    @GET("api/reports")
    suspend fun all(@Query("status") status: ReportStatus): List<ReportDto>

    @GET("api/reports/{reportId}")
    suspend fun details(@Path("reportId") reportId: Long): ReportDetailsDto

    @POST("api/reports/{reportId}/decision")
    suspend fun decide(
        @Path("reportId") reportId: Long,
        @Body request: DecideReportRequest,
    ): ReportDetailsDto

    @DELETE("api/reports/{reportId}")
    suspend fun delete(@Path("reportId") reportId: Long): Response<Unit>
}
