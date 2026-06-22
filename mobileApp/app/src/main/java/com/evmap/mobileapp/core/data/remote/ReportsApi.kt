package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.CreateReportRequest
import com.evmap.mobileapp.core.data.dto.ReportCategoryDto
import com.evmap.mobileapp.core.data.dto.ReportDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReportsApi {
    @POST("api/reports")
    suspend fun create(@Body request: CreateReportRequest): Long

    @GET("api/reports/categories")
    suspend fun categories(): List<ReportCategoryDto>

    @GET("api/reports")
    suspend fun all(): List<ReportDto>

    @PATCH("api/reports/{reportId}/status")
    suspend fun updateStatus(
        @Path("reportId") reportId: Long,
        @Query("status") status: String,
    ): Response<Unit>

    @DELETE("api/reports/{reportId}")
    suspend fun delete(@Path("reportId") reportId: Long): Response<Unit>
}
