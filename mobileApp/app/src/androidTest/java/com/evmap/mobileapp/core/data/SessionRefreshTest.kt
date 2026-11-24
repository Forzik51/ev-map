package com.evmap.mobileapp.core.data

import androidx.test.platform.app.InstrumentationRegistry
import com.evmap.mobileapp.core.data.dto.AuthResponse
import com.evmap.mobileapp.core.data.remote.AuthApi
import com.evmap.mobileapp.core.data.remote.AuthHeaderInterceptor
import com.evmap.mobileapp.core.data.remote.TokenRefreshAuthenticator
import com.evmap.mobileapp.core.data.session.TokenStore
import com.evmap.mobileapp.testTokenStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SessionRefreshTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = testTokenStore(context)
    private val account = AuthResponse("test-access", "test-refresh", expiresInSeconds = 3600, userId = 1, username = "test")

    @Test fun temporaryFailureKeepsSessionAndUnauthorizedRefreshClearsIt() {
        val previous = store.session()
        try {
            store.save(account)
            assertEquals(account.accessToken, TokenStore(context).accessToken())
            val encrypted = File(context.noBackupFilesDir, "evmap-session").readBytes().decodeToString()
            assertTrue(account.accessToken !in encrypted && account.refreshToken !in encrypted)
            val rejected = rejectedRequest()
            assertNull(authenticator { 503 to "{}" }.authenticate(null, rejected))
            assertEquals(account.refreshToken, store.refreshToken())
            assertNull(authenticator { 401 to "{}" }.authenticate(null, rejected))
            assertNull(store.session())
        } finally {
            if (previous == null) store.clear() else store.save(AuthResponse(
                previous.accessToken, previous.refreshToken, previous.tokenType,
                (previous.expiresAtMillis - System.currentTimeMillis()) / 1000, previous.userId, previous.username,
            ))
        }
    }

    @Test fun lateRefreshCannotRestoreLoggedOutSessionOrOverwriteNewLogin() {
        val started = CountDownLatch(1)
        val complete = CountDownLatch(1)
        val revoked = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        val previous = store.session()
        try {
            store.save(account)
            val rejected = rejectedRequest()
            val authenticator = authenticator { path ->
                if (path.endsWith("/logout")) {
                    revoked.countDown()
                    204 to ""
                } else {
                    started.countDown()
                    check(complete.await(10, TimeUnit.SECONDS))
                    200 to """{"accessToken":"rotated-access","refreshToken":"rotated-refresh","expiresInSeconds":3600,"userId":1,"username":"test"}"""
                }
            }
            val retry = executor.submit<Request?> { authenticator.authenticate(null, rejected) }
            assertTrue(started.await(10, TimeUnit.SECONDS))
            store.clear()
            store.save(account.copy(accessToken = "new-login", userId = 2))
            complete.countDown()
            assertNull(retry.get(10, TimeUnit.SECONDS))
            assertTrue(revoked.await(10, TimeUnit.SECONDS))
            assertEquals("new-login", store.accessToken())
        } finally {
            complete.countDown()
            executor.shutdownNow()
            if (previous == null) store.clear() else store.save(AuthResponse(
                previous.accessToken, previous.refreshToken, previous.tokenType,
                (previous.expiresAtMillis - System.currentTimeMillis()) / 1000, previous.userId, previous.username,
            ))
        }
    }

    private fun rejectedRequest(): Response = OkHttpClient.Builder()
        .addInterceptor(AuthHeaderInterceptor(store))
        .addInterceptor { chain -> response(chain.request(), 401, "{}") }
        .build().newCall(Request.Builder().url("https://example.invalid/api/events").build()).execute()

    private fun authenticator(reply: (String) -> Pair<Int, String>): TokenRefreshAuthenticator {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val (status, json) = reply(request.url.encodedPath)
            response(request, status, json)
        }.build()
        val api = Retrofit.Builder().baseUrl("https://example.invalid/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
            .build().create(AuthApi::class.java)
        return TokenRefreshAuthenticator(store, api)
    }

    private fun response(request: Request, status: Int, json: String): Response = Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(status).message("Fixture")
        .body(json.toResponseBody("application/json".toMediaType())).build()
}
