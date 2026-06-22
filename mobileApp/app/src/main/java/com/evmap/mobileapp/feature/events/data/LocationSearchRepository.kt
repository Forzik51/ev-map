package com.evmap.mobileapp.feature.events.data

import com.evmap.mobileapp.core.data.dto.SearchLocationDto
import com.evmap.mobileapp.core.data.remote.SearchApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationSearchRepository @Inject constructor(
    private val searchApi: SearchApi,
) {
    suspend fun searchLocations(query: String): Result<List<SearchLocationDto>> = runCatching {
        searchApi.search(query = query, type = "locations", limit = 25).locations
    }
}
