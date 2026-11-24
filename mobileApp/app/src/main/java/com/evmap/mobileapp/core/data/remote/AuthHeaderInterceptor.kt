package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.session.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID
import javax.inject.Inject

class AuthHeaderInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val session = tokenStore.session()
        val request = chain.request()
        val authenticatedRequest = if (session == null || request.header("Authorization") != null) {
            request
        } else {
            request.newBuilder()
                .header("Authorization", "Bearer ${session.accessToken}")
                .tag(UUID::class.java, session.loginId)
                .build()
        }
        return chain.proceed(authenticatedRequest)
    }
}
