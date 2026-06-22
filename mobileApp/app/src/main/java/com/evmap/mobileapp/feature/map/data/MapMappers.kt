package com.evmap.mobileapp.feature.map.data

import com.evmap.mobileapp.core.data.dto.MapEventItemDto
import core.ui.model.EventUi
import core.ui.model.MapPinUi

fun MapEventItemDto.toMapPinUi(): MapPinUi =
    MapPinUi(
        id = eventId.toString(),
        lat = point.lat,
        lng = point.lng,
        eventTitle = eventName,
    )

fun MapEventItemDto.toEventUi(): EventUi =
    EventUi(
        id = eventId.toString(),
        title = eventName,
        location = locationName.orEmpty(),
        startsAt = "",
        imageUrl = null,
        rating = ratingAvg.toFloat(),
        reviewCount = ratingsCount,
        description = categories.joinToString { it.name }.ifBlank { "Map event" },
    )
