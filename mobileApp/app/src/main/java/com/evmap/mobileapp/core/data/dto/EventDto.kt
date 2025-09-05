package com.evmap.mobileapp.core.data.dto

data class EventDto(
    val id: Long,
    val name: String, //title
    val description: String,
    val startsAt: String,  //date
    //val imageUrl: String? = null,
    val locationName: String, //locate
    //val rating: Float? = null,
    //val reviewCount: Int? = null,

)