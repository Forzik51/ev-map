package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.UserDto
import com.evmap.mobileapp.core.data.dto.UpdateProfileRequest
import com.evmap.mobileapp.core.data.dto.UserPhotoDto
import com.evmap.mobileapp.core.data.dto.UserShortDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface UsersApi {
    @GET("api/users")
    suspend fun getUsers(): List<UserDto>

    @GET("api/users/me")
    suspend fun getMe(): UserDto

    @PATCH("api/users/me")
    suspend fun updateMe(@Body profile: UpdateProfileRequest): UserDto

    @GET("api/users/{userId}")
    suspend fun getUser(
        @Path("userId") userId: Long,
    ): UserDto

    @GET("api/users/{userId}/followers")
    suspend fun getFollowers(
        @Path("userId") userId: Long,
    ): List<UserShortDto>

    @GET("api/users/{userId}/following")
    suspend fun getFollowing(
        @Path("userId") userId: Long,
    ): List<UserShortDto>

    @PATCH("api/users/{userId}/description")
    suspend fun updateDescription(
        @Path("userId") userId: Long,
        @Query("description") description: String,
    ): Response<Unit>

    @POST("api/users/{userId}/follow/{targetUserId}")
    suspend fun follow(
        @Path("userId") userId: Long,
        @Path("targetUserId") targetUserId: Long,
    ): Response<Unit>

    @DELETE("api/users/{userId}/follow/{targetUserId}")
    suspend fun unfollow(
        @Path("userId") userId: Long,
        @Path("targetUserId") targetUserId: Long,
    ): Response<Unit>

    @Multipart
    @POST("api/users/{userId}/photo")
    suspend fun uploadPhoto(
        @Path("userId") userId: Long,
        @Part file: MultipartBody.Part,
    ): UserPhotoDto

    @GET("api/users/{userId}/photo")
    suspend fun getPhoto(
        @Path("userId") userId: Long,
    ): UserPhotoDto

    @Multipart
    @PUT("api/users/{userId}/photo")
    suspend fun replacePhoto(
        @Path("userId") userId: Long,
        @Part file: MultipartBody.Part,
    ): UserPhotoDto

    @DELETE("api/users/{userId}/photo")
    suspend fun deletePhoto(
        @Path("userId") userId: Long,
    ): Response<Unit>
}
