package com.evmap.serverapp.features.event.api.dto

import java.math.BigDecimal

data class ViewEventReaction(
    val eventId: Long,
    val userId: Long,
    val username: String?,
    val rating: BigDecimal,
)
