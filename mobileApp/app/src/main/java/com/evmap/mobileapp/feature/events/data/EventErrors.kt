package com.evmap.mobileapp.feature.events.data

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

internal fun Throwable.eventError(): String = when(this) {
    is HttpException -> when(code()) {
        400 -> runCatching { response()?.errorBody()?.string()?.let { JSONObject(it).let { payload -> payload.optJSONObject("fieldErrors")?.let { fields -> fields.keys().asSequence().map { key->fields.optString(key) }.filter(String::isNotBlank).joinToString(". ") }?.takeIf(String::isNotBlank) ?: payload.optString("message") } } }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Перевірте заповнені поля"
        401 -> "Увійдіть знову, щоб продовжити"
        403 -> "Ви не маєте дозволу на цю дію"
        404 -> "Подію, фото або місце не знайдено"
        409 -> "Цей запит уже використано з іншими даними. Перевірте створену подію"
        410 -> "Подію вже видалено. Видаліть чернетку, щоб почати нову"
        413 -> "Виберіть фотографію до 5 МБ"
        426 -> "Оновіть застосунок до останньої версії"
        429 -> "Зачекайте секунду й повторіть спробу"
        else -> "Сервер тимчасово недоступний. Спробуйте ще раз"
    }
    is IOException -> "Перевірте інтернет-з’єднання та повторіть спробу"
    is IllegalArgumentException -> message ?: "Перевірте заповнені поля"
    else -> "Не вдалося завершити дію. Спробуйте ще раз"
}
