package com.evmap.mobileapp.feature.explore.data

import com.evmap.mobileapp.core.data.dto.SearchResponseDto
import com.evmap.mobileapp.core.data.remote.SearchApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    private val searchApi: SearchApi,
) {
    suspend fun search(query: String): Result<SearchResponseDto> = runCatching {
        searchApi.search(query = query, limit = 30)
    }
}
