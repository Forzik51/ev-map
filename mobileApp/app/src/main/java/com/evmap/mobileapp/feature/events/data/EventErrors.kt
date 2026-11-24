package com.evmap.mobileapp.feature.events.data

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

internal fun Throwable.eventError(): String = when (this) {
    is HttpException -> when (code()) {
        400 -> runCatching {
            response()?.errorBody()?.string()?.let { JSONObject(it).optString("message") }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Check the entered data."
        401 -> "Sign in again to continue."
        403 -> "You do not have permission to do this."
        404 -> "The event or photo is no longer available."
        413 -> "Choose a photo up to 5 MB."
        else -> "The server could not complete this request. Try again."
    }
    is IOException -> "Check your connection and try again."
    is IllegalArgumentException -> message ?: "Check the entered data."
    else -> "Could not complete this request. Try again."
}
