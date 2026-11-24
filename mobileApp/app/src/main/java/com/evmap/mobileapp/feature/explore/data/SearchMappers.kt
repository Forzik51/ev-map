package com.evmap.mobileapp.feature.explore.data

import com.evmap.mobileapp.core.data.dto.SearchEventDto
import com.evmap.mobileapp.core.data.dto.SearchUserDto
import core.ui.model.EventUi
import core.ui.model.ProfileUserUi

fun SearchEventDto.toEventUi(): EventUi =
    EventUi(
        id = id.toString(),
        title = name,
        location = locationName.orEmpty(),
        startsAt = startsAt,
        description = description,
        imageUrl = null,
        rating = ratingAvg,
        reviewCount = ratingsCount,
    )

fun SearchUserDto.toProfileUserUi(): ProfileUserUi =
    ProfileUserUi(
        id = id.toString(),
        username = username,
        title = listOfNotNull(name, surname).joinToString(" "),
        bio = pageDescription,
    )
