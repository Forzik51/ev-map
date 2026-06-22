package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.CreateListRequest
import com.evmap.mobileapp.core.data.dto.ListDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ListsApi {
    @POST("api/lists")
    suspend fun create(@Body request: CreateListRequest): Long

    @GET("api/lists/me")
    suspend fun mine(): List<ListDto>

    @GET("api/lists/{listId}/events")
    suspend fun eventIds(@Path("listId") listId: Long): List<Long>

    @POST("api/lists/{listId}/events/{eventId}")
    suspend fun addEvent(
        @Path("listId") listId: Long,
        @Path("eventId") eventId: Long,
    ): Response<Unit>

    @DELETE("api/lists/{listId}/events/{eventId}")
    suspend fun removeEvent(
        @Path("listId") listId: Long,
        @Path("eventId") eventId: Long,
    ): Response<Unit>

    @PATCH("api/lists/{listId}/name")
    suspend fun rename(
        @Path("listId") listId: Long,
        @Query("name") name: String,
    ): Response<Unit>

    @DELETE("api/lists/{listId}")
    suspend fun delete(@Path("listId") listId: Long): Response<Unit>
}
