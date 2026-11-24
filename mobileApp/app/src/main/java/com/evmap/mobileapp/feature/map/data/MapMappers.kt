package com.evmap.mobileapp.feature.map.data

import com.evmap.mobileapp.core.data.dto.MapEventDetailsDto
import com.evmap.mobileapp.core.data.dto.MapEventItemDto
import com.evmap.mobileapp.feature.feed.data.toAbsoluteMediaUrl
import core.ui.model.EventUi
import core.ui.model.MapPinUi

fun MapEventItemDto.toMapPinUi(): MapPinUi =
    MapPinUi(
        id = eventId.toString(),
        lat = point.lat,
        lng = point.lng,
        eventTitle = eventName,
    )

fun MapEventDetailsDto.toEventUi(): EventUi =
    EventUi(
        id = eventId.toString(),
        title = eventName,
        location = locationName.orEmpty(),
        startsAt = startsAt,
        imageUrl = imagePath.toAbsoluteMediaUrl(),
        rating = ratingAvg.takeIf { ratingsCount > 0 },
        reviewCount = ratingsCount,
        description = description,
    )
