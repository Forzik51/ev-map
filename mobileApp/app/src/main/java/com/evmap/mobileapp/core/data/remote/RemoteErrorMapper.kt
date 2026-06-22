package com.evmap.mobileapp.core.data.remote

import android.util.Log
import com.evmap.mobileapp.BuildConfig
import retrofit2.HttpException
import java.io.IOException

fun Throwable.toUserMessage(): String {
    if (BuildConfig.DEBUG) {
        Log.e("RemoteErrorMapper", "Remote call failed", this)
    }

    return when (this) {
        is HttpException -> when (code()) {
            400 -> "Check the entered data"
            401 -> "Invalid username/email or password"
            403 -> "Access denied"
            else -> message?.takeIf { it.isNotBlank() } ?: "Something went wrong"
        }
        is IOException -> {
            if (BuildConfig.DEBUG) {
                "Network connection failed: ${this::class.java.simpleName}: ${message.orEmpty()}"
            } else {
                "Network connection failed"
            }
        }
        else -> message?.takeIf { it.isNotBlank() } ?: "Something went wrong"
    }
}
