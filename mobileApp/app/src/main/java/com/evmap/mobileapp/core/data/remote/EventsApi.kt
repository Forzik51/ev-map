package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.CreateEventRequest
import com.evmap.mobileapp.core.data.dto.EventCategoryDto
import com.evmap.mobileapp.core.data.dto.EventCommentDto
import com.evmap.mobileapp.core.data.dto.EventDto
import com.evmap.mobileapp.core.data.dto.EventPhotoDto
import com.evmap.mobileapp.core.data.dto.EventReactionDto
import com.evmap.mobileapp.core.data.dto.PageResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface EventsApi {
    @GET("api/events")
    suspend fun getEvents(
        @Query("page") page: Int,
        @Query("size") size: Int = 20,
    ): PageResponse<EventDto>

    @GET("api/users/{userId}/events")
    suspend fun getUserEvents(
        @Path("userId") userId: Long,
        @Query("page") page: Int,
        @Query("size") size: Int,
    ): PageResponse<EventDto>

    @GET("api/events/feed")
    suspend fun getFeed(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("page") page: Int,
        @Query("size") size: Int = 20,
    ): PageResponse<EventDto>

    @GET("api/events/{id}")
    suspend fun getEvent(
        @Path("id") id: Long,
    ): EventDto

    @GET("api/events/categories")
    suspend fun getCategories(): List<EventCategoryDto>

    @POST("api/events")
    suspend fun createEvent(
        @Body request: CreateEventRequest,
    ): Long

    @PATCH("api/events/{id}")
    suspend fun updateEvent(
        @Path("id") id: Long,
        @Body request: CreateEventRequest,
    ): Response<Unit>

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(
        @Path("id") id: Long,
    ): Response<Unit>

    @GET("api/events/{id}/comments")
    suspend fun getComments(
        @Path("id") id: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<EventCommentDto>

    @POST("api/events/{id}/comments")
    suspend fun addComment(
        @Path("id") id: Long,
        @Query("comment") comment: String,
    ): Response<Unit>

    @GET("api/events/{id}/reactions")
    suspend fun getReactions(
        @Path("id") id: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<EventReactionDto>

    @POST("api/events/{id}/reactions")
    suspend fun addReaction(
        @Path("id") id: Long,
        @Query("reaction") reaction: String,
    ): Response<Unit>

    @POST("api/events/{id}/shares")
    suspend fun shareEvent(
        @Path("id") id: Long,
    ): Response<Unit>

    @DELETE("api/events/{id}/shares")
    suspend fun unshareEvent(
        @Path("id") id: Long,
    ): Response<Unit>

    @GET("api/events/{eventId}/photos")
    suspend fun getPhotos(
        @Path("eventId") eventId: Long,
    ): List<EventPhotoDto>

    @Multipart
    @POST("api/events/{eventId}/photos")
    suspend fun uploadPhoto(
        @Path("eventId") eventId: Long,
        @Query("uploadId") uploadId: String,
        @Part file: MultipartBody.Part,
    ): EventPhotoDto

    @PUT("api/events/{eventId}/photos/order")
    suspend fun orderPhotos(@Path("eventId") eventId: Long, @Body ids: List<Long>): Response<Unit>

    @DELETE("api/events/{eventId}/photos/{photoId}")
    suspend fun deletePhoto(
        @Path("eventId") eventId: Long,
        @Path("photoId") photoId: Long,
    ): Response<Unit>
}
