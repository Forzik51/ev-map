package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.SearchResponseDto
import com.evmap.mobileapp.core.data.dto.SearchSuggestionDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApi {
    @GET("api/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("type") type: String? = null,
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("limit") limit: Int? = null,
    ): SearchResponseDto

    @GET("api/search/suggestions")
    suspend fun suggestions(
        @Query("query") query: String,
        @Query("limit") limit: Int? = null,
    ): List<SearchSuggestionDto>

    @GET("api/search/popular")
    suspend fun popular(
        @Query("limit") limit: Int? = null,
    ): List<SearchSuggestionDto>
}
