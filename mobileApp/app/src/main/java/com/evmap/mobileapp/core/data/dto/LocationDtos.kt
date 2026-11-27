package com.evmap.mobileapp.core.data.dto

data class LocationDto(val id: Long? = null,val candidateId: String? = null,val name: String,val address: String? = null,val kind: String = "PLACE",val geometry: String,val lat: Double,val lng: Double)
data class CreateLocationRequest(val name: String,val kind: String,val geometry: String,val address: String? = null)
data class ResolveLocationRequest(val candidateId: String)
data class CategoryRequestDto(val id: Long? = null,val name: String,val status: String,val reason: String? = null)
data class EventTagDto(val id: Long,val name: String)
