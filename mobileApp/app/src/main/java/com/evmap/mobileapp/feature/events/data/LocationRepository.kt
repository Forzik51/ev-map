package com.evmap.mobileapp.feature.events.data

import com.evmap.mobileapp.core.data.remote.LocationsApi
import com.evmap.mobileapp.core.data.dto.*
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(retrofit: Retrofit) {
    val api: LocationsApi = retrofit.create(LocationsApi::class.java)
    suspend fun select(item: LocationDto) = if(item.id!=null) item else api.resolve(ResolveLocationRequest(requireNotNull(item.candidateId)))
}
