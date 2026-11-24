package com.evmap.mobileapp.feature.reports

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedInputStream
import java.io.Closeable
import java.io.DataInputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class ReportModerationNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test
    fun submitsAReportAndReviewsItWithRetryAndConflictHandling() {
        val store = testTokenStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val previousSession = store.session()
        val previousProxy = listOf("http.proxyHost", "http.proxyPort").associateWith(System::getProperty)
        ModerationFixture().use { fixture ->
            try {
                System.setProperty("http.proxyHost", "127.0.0.1")
                System.setProperty("http.proxyPort", fixture.port.toString())
                store.save(AuthResponse("fixture-user", "fixture-refresh", expiresInSeconds = 3600, userId = 1, username = "test"))
                ActivityScenario.launch(MainActivity::class.java).use {
                    awaitText("Festival")
                    compose.onNodeWithText("Festival").performClick()
                    awaitDescription("Report event")
                    compose.onNodeWithContentDescription("Report event").performClick()
                    awaitText("Spam")
                    compose.onNodeWithText("Spam").performClick()
                    compose.onNode(hasSetTextAction() and hasText("Description")).performTextReplacement("Duplicate listing")
                    compose.onNodeWithText("Submit report").performClick()
                    awaitDescription("Report event")
                    assertEquals(listOf(SubmittedReport(5L, 2L, "Duplicate listing")), fixture.submissions)
                }

                val adminClaim = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"role":"ADMIN"}""".encodeToByteArray())
                store.save(AuthResponse("fixture.$adminClaim.signature", "fixture-refresh", expiresInSeconds = 3600, userId = 3, username = "moderator"))
                ActivityScenario.launch(MainActivity::class.java).use {
                    awaitDescription("Profile")
                    compose.onNodeWithContentDescription("Profile").performClick()
                    awaitDescription("Settings")
                    compose.onNodeWithContentDescription("Settings").performClick()
                    compose.onNodeWithText("Admin moderation").performClick()
                    awaitText("Duplicate listing")
                    compose.onNodeWithText("Duplicate listing").performClick()
                    awaitEnabled("Open event")
                    compose.onNodeWithText("Open event").performClick()
                    awaitDescription("Report event")
                    awaitText("Event details for moderation")
                    compose.onNodeWithContentDescription("Back").performClick()
                    awaitEnabled("Confirm report")
                    compose.onNodeWithText("Confirm report").performClick()

                    val reason = compose.onNode(hasSetTextAction() and hasText("Reason"))
                    reason.performTextReplacement("Confirmed after reviewing the event")
                    compose.onNodeWithText("Save decision").performClick()
                    awaitText("The server could not complete the request. Try again.")
                    reason.assertTextContains("Confirmed after reviewing the event")
                    compose.onNodeWithText("Save decision").performClick()
                    awaitText("Spam · Resolved")
                    awaitText("Confirmed after reviewing the event")
                    assertEquals(
                        List(2) { SubmittedDecision(9L, "RESOLVED", "Confirmed after reviewing the event") },
                        fixture.decisions.toList(),
                    )

                    compose.onNodeWithContentDescription("Back").performClick()
                    compose.waitUntil(15_000) {
                        compose.onAllNodesWithText("Conflicting report").fetchSemanticsNodes().isNotEmpty() &&
                            compose.onAllNodesWithText("Duplicate listing").fetchSemanticsNodes().isEmpty()
                    }
                    compose.onNodeWithText("Resolved").performClick()
                    awaitText("Duplicate listing")
                    compose.onNodeWithText("Duplicate listing").performClick()
                    awaitText("Confirmed after reviewing the event")
                    compose.onNodeWithContentDescription("Back").performClick()

                    compose.onNodeWithText("Open").performClick()
                    awaitText("Conflicting report")
                    compose.onNodeWithText("Conflicting report").performClick()
                    awaitEnabled("Dismiss report")
                    compose.onNodeWithText("Dismiss report").performClick()
                    reason.performTextReplacement("My draft")
                    compose.onNodeWithText("Save decision").performClick()
                    awaitText("This report has already been reviewed.")
                    awaitText("Spam · Dismissed")
                    awaitText("Another moderator checked the event")
                    assertEquals(SubmittedDecision(8L, "DISMISSED", "My draft"), fixture.decisions.last())
                }
            } finally {
                previousProxy.forEach { (name, value) ->
                    if (value == null) System.clearProperty(name) else System.setProperty(name, value)
                }
                if (previousSession == null) store.clear() else store.save(AuthResponse(
                    previousSession.accessToken, previousSession.refreshToken, previousSession.tokenType,
                    (previousSession.expiresAtMillis - System.currentTimeMillis()) / 1000,
                    previousSession.userId, previousSession.username,
                ))
            }
        }
    }

    private fun awaitText(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitDescription(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitEnabled(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodes(hasText(text) and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
    }

    private data class SubmittedReport(val eventId: Long, val categoryId: Long, val description: String)
    private data class SubmittedDecision(val reportId: Long, val status: String, val reason: String)
    private data class Report(val id: Long, val description: String, var status: String = "OPEN", var reason: String? = null)

    private class ModerationFixture : Closeable {
        private val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        val port = server.localPort
        val submissions = CopyOnWriteArrayList<SubmittedReport>()
        val decisions = CopyOnWriteArrayList<SubmittedDecision>()
        private val reports = linkedMapOf(8L to Report(8L, "Conflicting report"))
        private val event = """{"id":5,"name":"Festival","description":"Event details for moderation","startsAt":"2030-03-22T10:00:00Z","userId":2}"""
        private val profile = """{"id":3,"name":"Moderator","email":"test@example.com","phone":"+48123456780","birthdate":"1990-01-01","username":"moderator","pageDescription":"","followersCount":0,"followingCount":0}"""

        init {
            thread(isDaemon = true, name = "Report moderation fixture") {
                while (!server.isClosed) runCatching { server.accept().use(::serve) }
            }
        }

        private fun serve(socket: Socket) {
            socket.soTimeout = 5_000
            val input = DataInputStream(BufferedInputStream(socket.getInputStream()))
            val (method, target) = input.readHttpLine().split(' ')
            val url = (if (target.startsWith("/")) BuildConfig.BASE_URL.trimEnd('/') + target else target).toHttpUrl()
            val headers = generateSequence { input.readHttpLine().takeIf(String::isNotEmpty) }.toList()
            val length = headers.firstOrNull { it.startsWith("Content-Length:", ignoreCase = true) }
                ?.substringAfter(':')?.trim()?.toInt() ?: 0
            val data = ByteArray(length).also(input::readFully).decodeToString()
            val (status, body) = respond(method, url, data)
            val bytes = body.encodeToByteArray()
            socket.getOutputStream().apply {
                write("HTTP/1.1 $status Fixture\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".encodeToByteArray())
                write(bytes)
                flush()
            }
        }

        private fun respond(method: String, url: HttpUrl, body: String): Pair<Int, String> = when (url.encodedPath) {
            "/api/events/feed" -> 200 to """{"content":[$event],"number":0,"size":20,"totalElements":1,"totalPages":1,"first":true,"last":true}"""
            "/api/events", "/api/events/5/comments", "/api/users/1/events" -> 200 to """{"content":[],"number":0,"size":20,"totalElements":0,"totalPages":0,"first":true,"last":true}"""
            "/api/events/5" -> 200 to event
            "/api/events/5/photos" -> 200 to "[]"
            "/api/users/me" -> 200 to profile
            "/api/reports/categories" -> 200 to """[{"id":2,"name":"Spam"}]"""
            "/api/reports" -> if (method == "POST") {
                val data = JSONObject(body)
                val submitted = SubmittedReport(data.getLong("eventId"), data.getLong("categoryId"), data.getString("description"))
                submissions += submitted
                reports[9L] = Report(9L, submitted.description)
                201 to "9"
            } else {
                val status = url.queryParameter("status")
                200 to reports.values.toList().asReversed().filter { it.status == status }
                    .joinToString(prefix = "[", postfix = "]") { summary(it).toString() }
            }
            "/api/reports/8", "/api/reports/9" -> 200 to details(reports.getValue(url.pathSegments.last().toLong()))
            "/api/reports/8/decision", "/api/reports/9/decision" -> {
                check(method == "POST")
                val report = reports.getValue(url.pathSegments[2].toLong())
                val payload = JSONObject(body)
                val decision = SubmittedDecision(report.id, payload.getString("status"), payload.getString("reason"))
                decisions += decision
                when {
                    report.id == 8L -> {
                        report.status = "DISMISSED"
                        report.reason = "Another moderator checked the event"
                        409 to "{}"
                    }
                    decisions.count { it.reportId == report.id } == 1 -> 503 to "{}"
                    else -> {
                        report.status = decision.status
                        report.reason = decision.reason
                        200 to details(report)
                    }
                }
            }
            else -> 404 to "{}"
        }

        private fun summary(report: Report): JSONObject = JSONObject()
            .put("id", report.id)
            .put("description", report.description)
            .put("eventId", 5)
            .put("eventName", "Festival")
            .put("categoryId", 2)
            .put("categoryName", "Spam")
            .put("status", report.status)
            .put("userId", 1)

        private fun details(report: Report): String {
            val decision = report.reason?.let { reason ->
                JSONObject()
                    .put("id", report.id)
                    .put("reason", reason)
                    .put("outcome", report.status)
                    .put("decidedAt", "2030-03-22T10:00:00Z")
                    .put("moderatorId", 3)
                    .put("moderatorUsername", "moderator")
                    .put("blockedUntil", JSONObject.NULL)
            }
            return """{"report":${summary(report)},"decisions":${if (decision == null) "[]" else "[$decision]"}}"""
        }

        private fun DataInputStream.readHttpLine(): String = buildString {
            while (true) {
                val byte = readUnsignedByte()
                if (byte == 10) break
                if (byte != 13) append(byte.toChar())
            }
        }

        override fun close() = server.close()
    }
}
