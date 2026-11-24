package com.evmap.mobileapp.feature.profile

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.core.app.ActivityOptionsCompat
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.NavGraph
import com.evmap.mobileapp.BuildConfig
import com.evmap.mobileapp.app.MainActivity
import com.evmap.mobileapp.core.data.dto.AuthResponse
import com.evmap.mobileapp.testTokenStore
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.DataInputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ProxySelector
import java.net.ServerSocket
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class ProfileEditNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun logoutLeavesTheProtectedGraphEvenWhenTheServerRejectsIt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = testTokenStore(context)
        val previous = store.session()
        val proxy = listOf("http.proxyHost", "http.proxyPort").associateWith(System::getProperty)
        ProfileFixture(byteArrayOf()).use { fixture ->
            try {
                System.setProperty("http.proxyHost", "127.0.0.1")
                System.setProperty("http.proxyPort", fixture.port.toString())
                store.save(AuthResponse("logout-access", "logout-refresh", expiresInSeconds = 3600, userId = 1, username = "original"))
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    compose.onNodeWithContentDescription("Profile").performClick()
                    compose.onNodeWithContentDescription("Settings").performClick()
                    compose.onNodeWithText("Log out").performClick()
                    awaitText("Sign up")
                    assertEquals(null, store.session())
                    scenario.recreate()
                    awaitText("Sign up")
                }
            } finally {
                proxy.forEach { (name, value) -> if (value == null) System.clearProperty(name) else System.setProperty(name, value) }
                if (previous == null) store.clear() else store.save(AuthResponse(
                    previous.accessToken, previous.refreshToken, previous.tokenType,
                    (previous.expiresAtMillis - System.currentTimeMillis()) / 1000, previous.userId, previous.username,
                ))
            }
        }
    }

    @Test fun editsProfileRetriesOnlyTheAvatarAndKeepsFollowStateAfterAnError() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val store = testTokenStore(context)
        val previousSession = store.session()
        val previousProxy = listOf("http.proxyHost", "http.proxyPort").associateWith(System::getProperty)
        val png = ByteArrayOutputStream().also { stream ->
            Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, stream)
        }.toByteArray()
        val images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val photo = checkNotNull(resolver.insert(images, ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "evmap-profile-test.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        }))
        checkNotNull(resolver.openOutputStream(photo)).use { it.write(png) }

        ProfileFixture(png).use { fixture ->
            try {
                System.setProperty("http.proxyHost", "127.0.0.1")
                System.setProperty("http.proxyPort", fixture.port.toString())
                assertTrue(ProxySelector.getDefault().select(URI(BuildConfig.BASE_URL)).any { proxy ->
                    (proxy.address() as? InetSocketAddress)?.port == fixture.port
                })
                store.save(AuthResponse("fixture-access", "fixture-refresh", expiresInSeconds = 3600, userId = 1, username = "original"))
                val picker = pickerReturning(photo)
                ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                    scenario.onActivity { activity ->
                        activity.setContent {
                            CompositionLocalProvider(LocalActivityResultRegistryOwner provides picker) {
                                NavGraph(rememberNavController())
                            }
                        }
                    }
                    compose.onNodeWithContentDescription("Profile").performClick()
                    openEditor()
                    compose.onNodeWithText("Change photo").performClick()
                    field("First name").performScrollTo().performTextReplacement("Олена")
                    field("Last name").performScrollTo().performTextReplacement("Коваль")
                    field("Username").performScrollTo().performTextReplacement("taken")
                    field("Phone").performScrollTo().performTextReplacement("+48123456789")
                    field("Bio").performScrollTo().performTextReplacement("Updated bio")
                    applyChanges()
                    awaitText("This username is already taken")
                    assertEquals(0, fixture.uploads.get())

                    field("Username").performScrollTo().performTextReplacement("renamed")
                    applyChanges()
                    awaitText("Profile details saved. Avatar update failed. Temporary upload error")
                    assertEquals(1, fixture.savedDetails.get())
                    applyChanges()
                    awaitText("Settings")
                    assertEquals(1, fixture.savedDetails.get())
                    assertEquals(2, fixture.uploads.get())
                    assertEquals("renamed", store.session()?.username)
                    compose.onNodeWithContentDescription("Back").performClick()
                    awaitText("Олена Коваль")
                    awaitText("Updated bio")

                    openEditor()
                    field("Phone").assertTextContains("+48123456789")
                    compose.onNodeWithText("Remove photo").performClick()
                    applyChanges()
                    awaitText("Settings")
                    assertEquals(1, fixture.removals.get())
                    compose.onNodeWithText("Profile").performClick()
                    awaitText("Choose photo")
                    compose.onNodeWithContentDescription("Back").performClick()
                    compose.onNodeWithContentDescription("Back").performClick()
                    awaitText("followers")
                    compose.onNodeWithText("followers").performClick()
                    awaitText("visitor")
                    compose.onNodeWithText("Follow").performClick()
                    awaitText("Access denied")
                    compose.onNodeWithText("Follow").assertExists().performClick()
                    awaitText("Following")
                }
            } finally {
                previousProxy.forEach { (name, value) -> if (value == null) System.clearProperty(name) else System.setProperty(name, value) }
                if (previousSession == null) store.clear() else store.save(AuthResponse(
                    previousSession.accessToken, previousSession.refreshToken, previousSession.tokenType,
                    (previousSession.expiresAtMillis - System.currentTimeMillis()) / 1000,
                    previousSession.userId, previousSession.username,
                ))
                resolver.delete(photo, null, null)
            }
        }
    }

    private fun openEditor() {
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Profile").performClick()
        awaitText("Edit profile")
        awaitText("First name")
    }

    private fun field(label: String) = compose.onNode(hasSetTextAction() and hasText(label))

    private fun applyChanges() = compose.onNodeWithText("Apply").performScrollTo().performClick()

    private fun awaitText(text: String) {
        compose.waitUntil(timeoutMillis = 15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun pickerReturning(uri: Uri) = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) {
                dispatchResult(requestCode, Activity.RESULT_OK, Intent().setData(uri))
            }
        }
    }

    private class ProfileFixture(private val png: ByteArray) : Closeable {
        private val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        val port = server.localPort
        val savedDetails = AtomicInteger()
        val uploads = AtomicInteger()
        val removals = AtomicInteger()
        private var followAttempts = 0
        private val profile = JSONObject("""{"id":1,"name":"Alice","surname":"Test","email":"test@example.com","phone":"+48123456780","birthdate":"1990-01-01","username":"original","pageDescription":"Old bio","path":"/media/old.png","followersCount":1,"followingCount":0}""")
        private val visitor = """[{"id":2,"username":"visitor","pageDescription":"","path":"/media/visitor.png"}]"""
        private val emptyPage = """{"content":[],"number":0,"size":20,"totalElements":0,"totalPages":0,"first":true,"last":true}"""

        init {
            thread(isDaemon = true, name = "Profile HTTP fixture") {
                while (!server.isClosed) runCatching {
                    server.accept().use { socket ->
                        socket.soTimeout = 5_000
                        val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
                        val (method, url) = input.readLineBytes().split(' ')
                        val headers = generateSequence { input.readLineBytes().takeIf(String::isNotEmpty) }.toList()
                        val length = headers.firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
                            ?.substringAfter(':')?.trim()?.toInt() ?: 0
                        val body = ByteArray(length).also(input::readFully)
                        val response = respond(method, url.toHttpUrl().encodedPath, body)
                        socket.getOutputStream().apply {
                            write("HTTP/1.1 ${response.status} Fixture\r\nContent-Type: ${response.type}\r\nContent-Length: ${response.body.size}\r\nConnection: close\r\n\r\n".encodeToByteArray())
                            write(response.body)
                            flush()
                        }
                    }
                }
            }
        }

        private fun respond(method: String, path: String, body: ByteArray): Reply = when {
            path.startsWith("/media/") -> Reply(200, png, "image/png")
            path in setOf("/api/events/feed", "/api/events", "/api/users/1/events") -> Reply(200, emptyPage)
            path == "/api/auth/logout" -> Reply(503, "{}")
            path == "/api/users/me" && method == "GET" -> Reply(200, profile.toString())
            path == "/api/users/me" && method == "PATCH" -> {
                val patch = JSONObject(body.decodeToString())
                if (patch.getString("username") == "taken") {
                    Reply(409, """{"message":"Conflict","fieldErrors":{"username":"This username is already taken"}}""")
                } else {
                    for (key in listOf("name", "surname", "username", "phone", "pageDescription")) profile.put(key, patch.get(key))
                    savedDetails.incrementAndGet()
                    Reply(200, profile.toString())
                }
            }
            path == "/api/users/1/photo" && method == "PUT" -> {
                check(body.toString(Charsets.ISO_8859_1).contains("name=\"file\""))
                if (uploads.incrementAndGet() == 1) Reply(500, """{"message":"Temporary upload error"}""")
                else {
                    profile.put("path", "/media/new.png")
                    Reply(200, """{"userId":1,"path":"/media/new.png"}""")
                }
            }
            path == "/api/users/1/photo" && method == "DELETE" -> {
                profile.put("path", JSONObject.NULL)
                removals.incrementAndGet()
                Reply(204, "")
            }
            path == "/api/users/1/followers" -> Reply(200, visitor)
            path == "/api/users/1/following" -> Reply(200, if (followAttempts > 1) visitor else "[]")
            path == "/api/users/1/follow/2" -> if (++followAttempts == 1) Reply(403, "{}") else Reply(204, "")
            else -> Reply(404, "{}")
        }

        override fun close() = server.close()

        private fun DataInputStream.readLineBytes(): String = buildString {
            while (true) {
                val byte = readUnsignedByte()
                if (byte == 10) break
                if (byte != 13) append(byte.toChar())
            }
        }

        private data class Reply(val status: Int, val body: ByteArray, val type: String) {
            constructor(status: Int, body: String) : this(status, body.encodeToByteArray(), "application/json")
        }
    }
}
