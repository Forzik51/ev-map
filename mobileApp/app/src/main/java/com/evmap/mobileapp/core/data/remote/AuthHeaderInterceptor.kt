package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.session.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthHeaderInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.accessToken()
        val request = chain.request()
        val authenticatedRequest = if (token.isNullOrBlank() || request.header("Authorization") != null) {
            request
        } else {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(authenticatedRequest)
    }
}
