package com.evmap.serverapp.features.event.application

import com.evmap.serverapp.features.event.api.dto.CreateEvent
import com.evmap.serverapp.features.event.domain.Event
import com.evmap.serverapp.shared.api.FormValidationException
import java.text.Normalizer
import java.time.ZoneId
import java.util.Locale

fun normalizedTag(value: String): String = Normalizer.normalize(value.trim().replace(Regex("\\s+"), " "), Normalizer.Form.NFKC).lowercase(Locale.ROOT)

fun addTagFilter(tags: List<String>,conditions: MutableList<String>,bindings: MutableList<Any>) {
    if(tags.isEmpty()) return
    if(tags.size>5 || tags.any { it.trim().length !in 1..30 }) throw FormValidationException(mapOf("tags" to "До 5 тегів, кожен до 30 символів"))
    val names=tags.map(::normalizedTag).distinct()
    conditions += "EXISTS(SELECT 1 FROM event_tag tag_filter JOIN tag tf ON tf.id=tag_filter.tag_id WHERE tag_filter.event_id=e.id AND tf.normalized_name IN (${names.joinToString(",") { "?" }}))"
    bindings.addAll(names)
}

fun CreateEvent.toEvent(userId: Long, id: Long? = null): Event {
    val fields = linkedMapOf<String,String>()
    if (name.trim().length !in 1..100) fields["name"] = "Укажіть назву до 100 символів"
    if (description.trim().length !in 1..2000) fields["description"] = "Укажіть опис до 2000 символів"
    if (categoryIds.distinct().size !in 1..3) fields["categoryIds"] = "Виберіть від 1 до 3 категорій"
    if (tags.size > 5 || tags.any { it.trim().length !in 1..30 }) fields["tags"] = "До 5 тегів, кожен до 30 символів"
    val zone = runCatching { ZoneId.of(timeZone) }.getOrNull()
    if (zone == null) fields["timeZone"] = "Виберіть часовий пояс"
    when (dateMode) {
        "UNSPECIFIED" -> if (startsAt != null || endsAt != null || startsOn != null || endsOn != null) fields["startsAt"] = "Вимкніть дату або очистьте розклад"
        "DATE" -> {
            if (startsOn == null) fields["startsOn"] = "Виберіть дату початку"
            if (startsOn != null && endsOn != null && endsOn.isBefore(startsOn)) fields["endsOn"] = "Завершення не може бути раніше початку"
            if (startsAt != null || endsAt != null) fields["startsAt"] = "Для дати без часу використайте календарні дні"
        }
        "DATETIME" -> {
            if (startsAt == null) fields["startsAt"] = "Виберіть дату й час початку"
            if (startsAt != null && endsAt != null && endsAt.isBefore(startsAt)) fields["endsAt"] = "Завершення не може бути раніше початку"
            if (startsOn != null || endsOn != null) fields["startsOn"] = "Для точного часу використайте дату й час"
        }
        else -> fields["dateMode"] = "Невідомий режим дати"
    }
    if (fields.isNotEmpty()) throw FormValidationException(fields)
    val effectiveStart = if (dateMode == "DATE") startsOn!!.atStartOfDay(zone!!).toInstant() else startsAt
    val effectiveEnd = if (dateMode == "DATE") (endsOn ?: startsOn)!!.plusDays(1).atStartOfDay(zone!!).toInstant().minusNanos(1000) else endsAt
    return Event(id, name.trim(), description.trim(), effectiveStart, effectiveEnd, locationId, userId, categoryIds.distinct(), dateMode = dateMode, startsOn = startsOn, endsOn = endsOn, timeZone = timeZone, tags = tags.map { it.trim() }.distinctBy(::normalizedTag))
}
