package com.evmap.mobileapp.feature.explore

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.evmap.mobileapp.BuildConfig
import com.evmap.mobileapp.app.MainActivity
import com.evmap.mobileapp.core.data.dto.AuthResponse
import com.evmap.mobileapp.testTokenStore
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.Closeable
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.ServerSocket
import java.net.URI
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class ExploreNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun feedOpensExploreAndAppliesCategoryAndSort() {
        val store = testTokenStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val previousSession = store.session()
        val previousHost = System.getProperty("http.proxyHost")
        val previousPort = System.getProperty("http.proxyPort")
        SearchFixture().use { fixture ->
            try {
                System.setProperty("http.proxyHost", "127.0.0.1")
                System.setProperty("http.proxyPort", fixture.port.toString())
                assertTrue(ProxySelector.getDefault().select(URI(BuildConfig.BASE_URL)).any { proxy ->
                    (proxy.address() as? InetSocketAddress)?.port == fixture.port
                })
                store.save(AuthResponse("fixture-access", "fixture-refresh", expiresInSeconds = 3600, userId = 1, username = "test"))
                ActivityScenario.launch(MainActivity::class.java).use {
                    val query = "музика & # %2F + ?"
                    val searchField = compose.onNode(hasSetTextAction())
                    searchField.performTextInput(query)
                    compose.onNodeWithContentDescription("Search").performClick()
                    awaitText("Matching event")
                    searchField.assertTextContains(query)
                    assertEquals(query, fixture.searches.last().queryParameter("query"))

                    compose.onNodeWithContentDescription("Filters").performClick()
                    compose.onNodeWithText("Relevance").performClick()
                    compose.onNodeWithText("Rating").performClick()
                    compose.onNodeWithText("All categories").performClick()
                    awaitText("Music")
                    compose.onNodeWithText("Music").performClick()
                    compose.onNodeWithText("Apply").performClick()
                    awaitText("Filtered event")
                    val filtered = fixture.searches.last()
                    assertEquals("events", filtered.queryParameter("type"))
                    assertEquals("rating", filtered.queryParameter("sort"))
                    assertEquals(listOf("7"), filtered.queryParameterValues("categoryIds"))

                    compose.onNodeWithText("Users").performClick()
                    awaitText("alice")
                    assertEquals(setOf("query", "type", "limit"), fixture.searches.last().queryParameterNames)
                    compose.onNodeWithContentDescription("Back").performClick()
                    awaitText("All events")
                    searchField.assertTextContains(query)
                    searchField.performTextReplacement("@alice")
                    searchField.performImeAction()
                    awaitText("alice")
                    assertEquals("@alice", fixture.searches.last().queryParameter("query"))
                    assertEquals("users", fixture.searches.last().queryParameter("type"))
                }
            } finally {
                restoreProperty("http.proxyHost", previousHost)
                restoreProperty("http.proxyPort", previousPort)
                if (previousSession == null) {
                    store.clear()
                } else {
                    store.save(AuthResponse(
                        previousSession.accessToken, previousSession.refreshToken, previousSession.tokenType,
                        (previousSession.expiresAtMillis - System.currentTimeMillis()) / 1000,
                        previousSession.userId, previousSession.username,
                    ))
                }
            }
        }
    }

    private fun awaitText(text: String) {
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun restoreProperty(name: String, value: String?) {
        if (value == null) System.clearProperty(name) else System.setProperty(name, value)
    }

    private class SearchFixture : Closeable {
        private val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        val port = server.localPort
        val searches = CopyOnWriteArrayList<HttpUrl>()

        init {
            thread(isDaemon = true, name = "Explore HTTP fixture") {
                while (!server.isClosed) {
                    runCatching {
                        server.accept().use { connection ->
                            connection.soTimeout = 5_000
                            val input = connection.getInputStream().bufferedReader()
                            val url = input.readLine().split(' ')[1].toHttpUrl()
                            while (!input.readLine().isNullOrBlank()) Unit
                            val bytes = body(url).encodeToByteArray()
                            connection.getOutputStream().apply {
                                write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".encodeToByteArray())
                                write(bytes)
                                flush()
                            }
                        }
                    }
                }
            }
        }

        private fun body(url: HttpUrl): String = when (url.encodedPath) {
            "/api/events/feed" -> """{"content":[],"number":0,"size":20,"totalElements":0,"totalPages":0,"first":true,"last":true}"""
            "/api/events/categories" -> """[{"id":7,"name":"Music"}]"""
            "/api/search" -> {
                searches += url
                if (url.queryParameter("type") == "users") {
                    """{"type":"users","limit":30,"users":[{"id":2,"username":"alice","name":"Alice","pageDescription":""}]}"""
                } else {
                    val title = if (url.queryParameter("categoryIds") == "7") "Filtered event" else "Matching event"
                    """{"type":"events","limit":30,"events":[{"id":3,"name":"$title","description":"Test","startsAt":"2030-03-22T10:00:00Z","ratingAvg":4.5,"ratingsCount":2,"categories":[]}]}"""
                }
            }
            else -> "{}"
        }

        override fun close() = server.close()
    }
}
