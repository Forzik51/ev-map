package com.evmap.mobileapp.core.data.session

import android.content.Context
import android.util.Base64
import com.evmap.mobileapp.core.data.dto.AuthResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresAtMillis: Long,
    val userId: Long,
    val username: String,
) {
    val isAdmin: Boolean
        get() = role == "ADMIN"

    val role: String
        get() = accessToken.roleClaim() ?: "USER"
}

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("evmap_auth", Context.MODE_PRIVATE)

    fun session(): AuthSession? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: return null
        return AuthSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = prefs.getString(KEY_TOKEN_TYPE, "Bearer") ?: "Bearer",
            expiresAtMillis = prefs.getLong(KEY_EXPIRES_AT, 0L),
            userId = prefs.getLong(KEY_USER_ID, 0L),
            username = prefs.getString(KEY_USERNAME, "") ?: "",
        )
    }

    fun accessToken(): String? = session()?.accessToken

    fun refreshToken(): String? = session()?.refreshToken

    fun isLoggedIn(): Boolean = session() != null

    fun isAdmin(): Boolean = session()?.isAdmin == true

    fun save(response: AuthResponse) {
        val expiresAtMillis = System.currentTimeMillis() + response.expiresInSeconds * 1000
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, response.accessToken)
            .putString(KEY_REFRESH_TOKEN, response.refreshToken)
            .putString(KEY_TOKEN_TYPE, response.tokenType)
            .putLong(KEY_EXPIRES_AT, expiresAtMillis)
            .putLong(KEY_USER_ID, response.userId)
            .putString(KEY_USERNAME, response.username)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_TOKEN_TYPE = "token_type"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
    }
}

private fun String.roleClaim(): String? =
    runCatching {
        val payload = split(".").getOrNull(1) ?: return null
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        JSONObject(String(decoded, Charsets.UTF_8)).optString("role").takeIf { it.isNotBlank() }
    }.getOrNull()
