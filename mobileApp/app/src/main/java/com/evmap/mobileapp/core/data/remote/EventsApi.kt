package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.EventDto
import com.evmap.mobileapp.core.data.dto.PageResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface EventsApi {
    @GET("api/events")
    suspend fun getEvents(
        @Query("page") page: Int,
        @Query("size") size: Int = 20,
        //@Query("sort") sort: String = "startsAt,desc"
    ): PageResponse<EventDto>
}