package com.evmap.mobileapp.feature.feed.data

import com.evmap.mobileapp.BuildConfig
import com.evmap.mobileapp.core.data.dto.EventDto
import core.ui.model.EventUi
import core.ui.model.FeedItemUi
import java.time.Instant

fun EventDto.toFeedItemUi() = FeedItemUi(
    id = id.toString(),
    event = toEventUi(),
    timestamp = createdAt?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0L,
    isPromoted = false
)


fun EventDto.toEventUi(): EventUi = EventUi(
    id = id.toString(),
    title = name,
    location = locationName.orEmpty(),
    startsAt = com.evmap.mobileapp.core.ui.formatEventSchedule(startsAt, dateMode, startsOn, endsOn, timeZone),
    description = description,
    imageUrl = imagePath.toAbsoluteMediaUrl(),
    rating = ratingAvg,
    reviewCount = commentsCount
)

fun String?.toEpochMillisOrNow(): Long =
    this?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
        ?: System.currentTimeMillis()

fun String?.toAbsoluteMediaUrl(): String? {
    if (isNullOrBlank()) return null
    if (startsWith("http://") || startsWith("https://")) return this
    return BuildConfig.BASE_URL.trimEnd('/') + "/" + trimStart('/')
}
