package com.evmap.mobileapp.feature.saved.data

import com.evmap.mobileapp.core.data.dto.ListDto
import core.ui.model.SavedGroupUi

fun ListDto.toSavedGroupUi(): SavedGroupUi =
    SavedGroupUi(
        id = id.toString(),
        name = name,
        count = eventsCount,
    )
