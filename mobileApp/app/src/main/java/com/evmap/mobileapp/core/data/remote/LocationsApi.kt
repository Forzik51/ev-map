package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.*
import retrofit2.http.*

interface LocationsApi {
    @GET("api/event-taxonomy/tags") suspend fun tags(@Query("query") query: String): List<EventTagDto>
    @GET("api/locations/search") suspend fun search(@Query("query") query: String): List<LocationDto>
    @GET("api/locations/{id}") suspend fun get(@Path("id") id: Long): LocationDto
    @POST("api/locations/resolve") suspend fun resolve(@Body request: ResolveLocationRequest): LocationDto
    @POST("api/locations") suspend fun create(@Body request: CreateLocationRequest): LocationDto
    @POST("api/event-taxonomy/category-requests") suspend fun propose(@Body request: Map<String,String>): CategoryRequestDto
    @GET("api/event-taxonomy/category-requests") suspend fun categoryRequests(@Query("all") all: Boolean): List<CategoryRequestDto>
    @POST("api/event-taxonomy/category-requests/{id}/decision") suspend fun decide(@Path("id") id: Long,@Body decision: Map<String,@JvmSuppressWildcards Any>)
}
