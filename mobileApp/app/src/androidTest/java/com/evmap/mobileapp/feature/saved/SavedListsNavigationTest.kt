package com.evmap.mobileapp.feature.saved

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.DataInputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.ServerSocket
import java.net.URI
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class SavedListsNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun createsSavesRenamesRemovesAndDeletesThroughTheApp() {
        val store = testTokenStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val previousSession = store.session()
        val previousProxy = listOf("http.proxyHost", "http.proxyPort").associateWith(System::getProperty)
        ListsFixture().use { fixture ->
            try {
                System.setProperty("http.proxyHost", "127.0.0.1")
                System.setProperty("http.proxyPort", fixture.port.toString())
                assertTrue(ProxySelector.getDefault().select(URI(BuildConfig.BASE_URL)).any { proxy ->
                    (proxy.address() as? InetSocketAddress)?.port == fixture.port
                })
                store.save(AuthResponse("fixture-access", "fixture-refresh", expiresInSeconds = 3600, userId = 1, username = "test"))
                ActivityScenario.launch(MainActivity::class.java).use {
                    openSavedLists()
                    awaitText("No saved lists yet")
                    compose.onNodeWithContentDescription("Create list").performClick()
                    enterName("Weekend")
                    compose.onNodeWithText("Create").performClick()
                    awaitText("Weekend")

                    backToFeed()
                    openPicker()
                    compose.onNodeWithText("Weekend").performClick()
                    awaitText("Added to Weekend")
                    compose.onNodeWithText("Done").performClick()

                    openSavedLists()
                    awaitText("1 event")
                    compose.onNodeWithText("Weekend").performClick()
                    awaitText("Festival")
                    compose.onNodeWithContentDescription("Manage list").performClick()
                    compose.onNodeWithText("Rename list").performClick()
                    enterName("Weekend & friends")
                    compose.onNodeWithText("Rename").performClick()
                    awaitText("Weekend & friends")

                    compose.onNodeWithContentDescription("More options").performClick()
                    compose.onNodeWithText("Remove from list").performClick()
                    awaitText("The server could not complete the request. Try again.")
                    compose.onNodeWithText("Festival").assertExists()
                    compose.onNodeWithText("1 event").assertExists()
                    compose.onNodeWithContentDescription("Grid view").performClick()
                    compose.onNodeWithContentDescription("Options for Festival").performClick()
                    compose.onNodeWithText("Remove from list").performClick()
                    awaitText("No events in this list")
                    compose.onNodeWithText("0 events").assertExists()

                    compose.onNodeWithContentDescription("Manage list").performClick()
                    compose.onNodeWithText("Delete list").performClick()
                    awaitText("Delete list?")
                    compose.onNodeWithText("Delete").performClick()
                    awaitText("No saved lists yet")

                    backToFeed()
                    openPicker()
                    awaitText("Create a list to save this event.")
                    compose.onNodeWithText("Create list").performClick()
                    enterName("From picker")
                    compose.onNodeWithText("Create").performClick()
                    awaitText("From picker")
                    compose.onNodeWithText("From picker").performClick()
                    awaitText("Added to From picker")
                }
            } finally {
                previousProxy.forEach { (name, value) -> if (value == null) System.clearProperty(name) else System.setProperty(name, value) }
                if (previousSession == null) store.clear() else store.save(AuthResponse(
                    previousSession.accessToken, previousSession.refreshToken, previousSession.tokenType,
                    (previousSession.expiresAtMillis - System.currentTimeMillis()) / 1000,
                    previousSession.userId, previousSession.username,
                ))
            }
        }
    }

    private fun openSavedLists() {
        compose.onNodeWithContentDescription("Profile").performClick()
        compose.waitUntil(timeoutMillis = 15_000) {
            compose.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Saved events").performClick()
    }

    private fun backToFeed() {
        repeat(2) { compose.onNodeWithContentDescription("Back").performClick() }
        compose.onNodeWithContentDescription("Feed").performClick()
    }

    private fun openPicker() {
        awaitText("Festival")
        compose.onNodeWithContentDescription("Save event").performClick()
        awaitText("Save to list")
    }

    private fun enterName(name: String) = compose.onNode(hasSetTextAction() and hasText("List name")).performTextReplacement(name)

    private fun awaitText(text: String) {
        compose.waitUntil(timeoutMillis = 15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private class ListsFixture : Closeable {
        private val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        val port = server.localPort
        private var name: String? = null
        private var saved = false
        private var removalAttempts = 0
        private val event = """{"id":5,"name":"Festival","description":"A saved event","startsAt":"2030-03-22T10:00:00Z","userId":2}"""
        private val profile = """{"id":1,"name":"Test","email":"test@example.com","phone":"+48123456780","birthdate":"1990-01-01","username":"test","pageDescription":"","followersCount":0,"followingCount":0}"""

        init {
            thread(isDaemon = true, name = "Saved lists HTTP fixture") {
                while (!server.isClosed) runCatching {
                    server.accept().use { socket ->
                        socket.soTimeout = 5_000
                        val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
                        val (method, requestUrl) = input.readLineBytes().split(' ')
                        val headers = generateSequence { input.readLineBytes().takeIf(String::isNotEmpty) }.toList()
                        val length = headers.firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
                            ?.substringAfter(':')?.trim()?.toInt() ?: 0
                        val data = ByteArray(length).also(input::readFully).decodeToString()
                        val (status, body) = respond(method, requestUrl.toHttpUrl(), data)
                        val bytes = body.encodeToByteArray()
                        socket.getOutputStream().apply {
                            write("HTTP/1.1 $status Fixture\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".encodeToByteArray())
                            write(bytes)
                            flush()
                        }
                    }
                }
            }
        }

        private fun summary() = JSONObject().put("id", 7).put("name", name).put("userId", 1).put("eventsCount", if (saved) 1 else 0)

        private fun respond(method: String, url: HttpUrl, body: String): Pair<Int, String> = when (url.encodedPath) {
            "/api/events/feed" -> 200 to """{"content":[$event],"number":0,"size":20,"totalElements":1,"totalPages":1,"first":true,"last":true}"""
            "/api/events", "/api/users/1/events" -> 200 to """{"content":[],"number":0,"size":20,"totalElements":0,"totalPages":0,"first":true,"last":true}"""
            "/api/users/me" -> 200 to profile
            "/api/lists/me" -> 200 to if (name == null) "[]" else "[${summary()}]"
            "/api/lists" -> {
                check(method == "POST")
                name = JSONObject(body).getString("name")
                201 to "7"
            }
            "/api/lists/7/name" -> {
                check(method == "PATCH")
                name = url.queryParameter("name")
                204 to ""
            }
            "/api/lists/7/events/5" -> when {
                method == "POST" -> {
                    saved = true
                    204 to ""
                }
                ++removalAttempts == 1 -> 503 to "{}"
                else -> {
                    saved = false
                    204 to ""
                }
            }
            "/api/lists/7" -> if (method == "DELETE") {
                name = null
                saved = false
                204 to ""
            } else {
                val events = if (saved) "[$event]" else "[]"
                200 to """{"list":${summary()},"events":$events}"""
            }
            else -> 404 to "{}"
        }

        private fun DataInputStream.readLineBytes(): String = buildString {
            while (true) {
                val byte = readUnsignedByte()
                if (byte == 10) break
                if (byte != 13) append(byte.toChar())
            }
        }

        override fun close() = server.close()
    }
}
