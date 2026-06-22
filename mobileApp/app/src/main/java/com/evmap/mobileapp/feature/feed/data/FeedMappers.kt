package com.evmap.mobileapp.feature.feed.data

import com.evmap.mobileapp.BuildConfig
import com.evmap.mobileapp.core.data.dto.EventDto
import core.ui.model.EventUi
import core.ui.model.FeedItemUi
import java.time.Instant

fun EventDto.toFeedItemUi() = FeedItemUi(
    id = id.toString(),
    event = toEventUi(),
    timestamp = startsAt.toEpochMillisOrNow(),
    isPromoted = false
)


fun EventDto.toEventUi(): EventUi = EventUi(
    id = id.toString(),
    title = name,
    location = locationName.orEmpty(),
    startsAt = startsAt,
    description = description,
    imageUrl = imagePath.toAbsoluteMediaUrl(),
    rating = ratingsCount.takeIf { it > 0 }?.toFloat(),
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
