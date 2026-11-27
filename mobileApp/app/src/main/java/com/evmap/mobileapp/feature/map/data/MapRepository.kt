package com.evmap.mobileapp.feature.map.data

import com.evmap.mobileapp.core.data.remote.MapApi
import core.ui.model.MapViewport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapRepository @Inject constructor(
    private val mapApi: MapApi,
) {
    suspend fun bounds(viewport: MapViewport, categoryIds: List<Long>?=null, tags: List<String>?=null) = request {
        acrossBounds(viewport) { bounds ->
            mapApi.bounds(bounds.south, bounds.west, bounds.north, bounds.east, DEFAULT_LIMIT, categoryIds, tags)
        }.distinctBy { it.eventId }
    }

    suspend fun clusters(viewport: MapViewport, categoryIds: List<Long>?=null, tags: List<String>?=null) = request {
        acrossBounds(viewport) { bounds ->
            mapApi.clusters(bounds.south, bounds.west, bounds.north, bounds.east, bounds.zoom, categoryIds, tags)
        }
    }

    suspend fun details(eventId: Long) = request { mapApi.details(eventId) }

    suspend fun search(query: String, lat: Double, lng: Double, categoryIds: List<Long>?=null, tags: List<String>?=null) = request {
        mapApi.search(query = query, lat = lat, lng = lng, limit = DEFAULT_LIMIT, categoryIds=categoryIds, tags=tags)
    }

    private suspend fun <T> acrossBounds(viewport: MapViewport, load: suspend (MapViewport) -> List<T>): List<T> =
        coroutineScope { viewport.apiBounds().map { bounds -> async { load(bounds) } }.awaitAll().flatten() }

    private suspend fun <T> request(load: suspend () -> T): Result<T> = try {
        Result.success(load())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    private companion object {
        const val DEFAULT_LIMIT = 50
    }
}
