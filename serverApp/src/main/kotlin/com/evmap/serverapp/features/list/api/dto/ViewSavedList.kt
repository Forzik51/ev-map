package com.evmap.serverapp.features.list.api.dto

import com.evmap.serverapp.features.event.api.dto.ViewEvent

data class ViewSavedList(val list: ViewList, val events: List<ViewEvent>)
