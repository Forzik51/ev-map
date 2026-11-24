package com.evmap.mobileapp.core.data.session

import android.content.Context
import android.util.Base64
import android.util.Log
import com.evmap.mobileapp.core.data.dto.AuthResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val expiresAtMillis: Long,
    val userId: Long,
    val username: String,
    internal val loginId: UUID = UUID.randomUUID(),
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
    private val storage = SessionStorage(context)
    private val current = MutableStateFlow(restore())
    val sessions = current.asStateFlow()

    private fun restore(): AuthSession? {
        val restored = try {
            storage.read()?.let(::JSONObject)?.toSession()
        } catch (_: Exception) {
            runCatching(storage::clear)
            null
        }
        if (restored != null) {
            if (prefs.contains(KEY_ACCESS_TOKEN)) prefs.edit().clear().commit()
            return restored
        }
        val legacy = legacySession() ?: return null
        return try {
            storage.write(legacy.toJson())
            prefs.edit().clear().commit()
            legacy
        } catch (_: Exception) {
            prefs.edit().clear().commit()
            null
        }
    }

    private fun legacySession(): AuthSession? {
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

    fun session(): AuthSession? = current.value
    fun accessToken(): String? = session()?.accessToken

    fun refreshToken(): String? = session()?.refreshToken

    fun isLoggedIn(): Boolean = session() != null

    fun isAdmin(): Boolean = session()?.isAdmin == true

    @Synchronized
    fun save(response: AuthResponse) {
        save(response.toSession())
    }

    @Synchronized
    internal fun replaceIfCurrent(expected: AuthSession, response: AuthResponse): Boolean {
        val active = current.value ?: return false
        if (active.loginId != expected.loginId || active.refreshToken != expected.refreshToken ||
            active.userId != response.userId) return false
        save(response.toSession().copy(loginId = active.loginId,
            username = if (active.username == expected.username) response.username else active.username))
        return true
    }

    @Synchronized
    fun updateUsername(userId: Long, username: String) {
        val active = session() ?: return
        if (active.userId == userId && active.username != username) {
            save(active.copy(username = username))
        }
    }

    fun clear() { takeAndClear() }

    @Synchronized
    internal fun takeAndClear(): AuthSession? {
        val active = current.value
        current.value = null
        runCatching(storage::clear).onFailure { Log.w("TokenStore", "Could not clear session storage") }
        if (prefs.contains(KEY_ACCESS_TOKEN)) prefs.edit().clear().commit()
        return active
    }

    @Synchronized
    internal fun clearIfCurrent(expected: AuthSession) {
        val active = current.value ?: return
        if (active.loginId == expected.loginId && active.refreshToken == expected.refreshToken) clear()
    }

    private fun save(session: AuthSession) {
        storage.write(session.toJson())
        current.value = session
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_TOKEN_TYPE = "token_type"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
    }

    private fun AuthSession.toJson(): String = JSONObject()
        .put(KEY_ACCESS_TOKEN, accessToken)
        .put(KEY_REFRESH_TOKEN, refreshToken)
        .put(KEY_TOKEN_TYPE, tokenType)
        .put(KEY_EXPIRES_AT, expiresAtMillis)
        .put(KEY_USER_ID, userId)
        .put(KEY_USERNAME, username)
        .toString()

    private fun JSONObject.toSession() = AuthSession(
        getString(KEY_ACCESS_TOKEN), getString(KEY_REFRESH_TOKEN), getString(KEY_TOKEN_TYPE),
        getLong(KEY_EXPIRES_AT), getLong(KEY_USER_ID), getString(KEY_USERNAME),
    )

    private fun AuthResponse.toSession() = AuthSession(
        accessToken, refreshToken, tokenType, System.currentTimeMillis() + expiresInSeconds * 1000,
        userId, username,
    )
}

private fun String.roleClaim(): String? =
    runCatching {
        val payload = split(".").getOrNull(1) ?: return null
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        JSONObject(String(decoded, Charsets.UTF_8)).optString("role").takeIf { it.isNotBlank() }
    }.getOrNull()
