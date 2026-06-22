package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.MapClusterDto
import com.evmap.mobileapp.core.data.dto.MapEventDetailsDto
import com.evmap.mobileapp.core.data.dto.MapEventItemDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MapApi {
    @GET("api/map/events/nearby")
    suspend fun nearby(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusMeters") radiusMeters: Double? = null,
        @Query("limit") limit: Int? = null,
    ): List<MapEventItemDto>

    @GET("api/map/events/bounds")
    suspend fun bounds(
        @Query("minLat") minLat: Double,
        @Query("minLng") minLng: Double,
        @Query("maxLat") maxLat: Double,
        @Query("maxLng") maxLng: Double,
        @Query("limit") limit: Int? = null,
    ): List<MapEventItemDto>

    @GET("api/map/events/clusters")
    suspend fun clusters(
        @Query("minLat") minLat: Double,
        @Query("minLng") minLng: Double,
        @Query("maxLat") maxLat: Double,
        @Query("maxLng") maxLng: Double,
        @Query("zoom") zoom: Int,
    ): List<MapClusterDto>

    @GET("api/map/events/{eventId}")
    suspend fun details(
        @Path("eventId") eventId: Long,
    ): MapEventDetailsDto

    @GET("api/map/events/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("limit") limit: Int? = null,
    ): List<MapEventItemDto>
}
