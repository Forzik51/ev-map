package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.RefreshTokenRequest
import com.evmap.mobileapp.core.data.session.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

class TokenRefreshAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    @Named("refreshAuthApi") private val authApi: AuthApi,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_AUTH_ATTEMPTS) return null
        val requestLogin = response.request.tag(UUID::class.java) ?: return null
        val requestBearer = response.request.header("Authorization") ?: return null

        synchronized(this) {
            val active = tokenStore.session()?.takeIf { it.loginId == requestLogin } ?: return null
            if (requestBearer != "Bearer ${active.accessToken}") {
                return response.request.withBearer(active.accessToken)
            }

            val refreshed = try {
                runBlocking { authApi.refresh(RefreshTokenRequest(active.refreshToken)) }
            } catch (error: Exception) {
                if (error is HttpException && error.code() == 401) tokenStore.clearIfCurrent(active)
                return null
            }
            if (!runCatching { tokenStore.replaceIfCurrent(active, refreshed) }.getOrDefault(false)) {
                // The server may have rotated the token after logout began.
                runCatching { runBlocking { authApi.logout(RefreshTokenRequest(refreshed.refreshToken)) } }
                return null
            }
            return response.request.withBearer(refreshed.accessToken)
        }
    }

    private fun Request.withBearer(token: String): Request =
        newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

    private fun responseCount(response: Response): Int {
        var count = 1
        var priorResponse = response.priorResponse
        while (priorResponse != null) {
            count++
            priorResponse = priorResponse.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_AUTH_ATTEMPTS = 2
    }
}
