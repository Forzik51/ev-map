package com.evmap.mobileapp.core.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun formatEventSchedule(startsAt: String?,mode: String = "DATETIME",startsOn: String? = null,endsOn: String? = null,timeZone: String = ZoneId.systemDefault().id): String {
    if(mode == "UNSPECIFIED" || startsAt == null && startsOn == null) return "Дату не визначено"
    return runCatching {
        if(mode == "DATE") {
            val start=LocalDate.parse(startsOn).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            if(endsOn == null || endsOn == startsOn) start else "$start — ${LocalDate.parse(endsOn).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}"
        } else Instant.parse(startsAt).atZone(ZoneId.of(timeZone)).format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
    }.getOrDefault("Дату не визначено")
}
