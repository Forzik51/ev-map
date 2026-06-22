package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.SendEmailRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationsApi {
    @POST("api/notifications/email/all")
    suspend fun sendToAll(@Body request: SendEmailRequest): Response<Unit>

    @POST("api/notifications/email/user/{userId}")
    suspend fun sendToUser(
        @Path("userId") userId: Long,
        @Body request: SendEmailRequest,
    ): Response<Unit>
}
