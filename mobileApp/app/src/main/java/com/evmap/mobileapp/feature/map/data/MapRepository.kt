package com.evmap.mobileapp.feature.map.data

import com.evmap.mobileapp.core.data.dto.MapEventItemDto
import com.evmap.mobileapp.core.data.remote.MapApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapRepository @Inject constructor(
    private val mapApi: MapApi,
) {
    suspend fun nearby(lat: Double, lng: Double): Result<List<MapEventItemDto>> = runCatching {
        mapApi.nearby(lat = lat, lng = lng, radiusMeters = DEFAULT_RADIUS_METERS, limit = DEFAULT_LIMIT)
    }

    suspend fun search(query: String, lat: Double, lng: Double): Result<List<MapEventItemDto>> = runCatching {
        mapApi.search(query = query, lat = lat, lng = lng, limit = DEFAULT_LIMIT)
    }

    private companion object {
        const val DEFAULT_RADIUS_METERS = 25_000.0
        const val DEFAULT_LIMIT = 50
    }
}
