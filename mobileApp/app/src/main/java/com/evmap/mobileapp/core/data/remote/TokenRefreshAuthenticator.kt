package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.RefreshTokenRequest
import com.evmap.mobileapp.core.data.session.TokenStore
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named

class TokenRefreshAuthenticator @Inject constructor(
    private val tokenStore: TokenStore,
    @Named("refreshAuthApi") private val authApi: AuthApi,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= MAX_AUTH_ATTEMPTS) return null
        val refreshToken = tokenStore.refreshToken() ?: return null
        val requestToken = response.request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        synchronized(this) {
            val latestToken = tokenStore.accessToken()
            if (!latestToken.isNullOrBlank() && latestToken != requestToken) {
                return response.request.withBearer(latestToken)
            }

            val refreshed = runBlocking {
                runCatching {
                    authApi.refresh(RefreshTokenRequest(refreshToken))
                }.getOrNull()
            } ?: run {
                tokenStore.clear()
                return null
            }

            tokenStore.save(refreshed)
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
