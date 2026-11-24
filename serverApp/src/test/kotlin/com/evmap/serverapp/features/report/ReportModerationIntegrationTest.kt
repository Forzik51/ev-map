package com.evmap.serverapp.features.report

import com.evmap.serverapp.features.auth.PasswordResetIntegrationTest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@EnabledIfEnvironmentVariable(named = "EVMAP_TEST_DB_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReportModerationIntegrationTest {
    @LocalServerPort private var port = 0
    @Autowired private lateinit var db: DSLContext
    @Autowired private lateinit var json: ObjectMapper
    @Autowired private lateinit var passwords: PasswordEncoder
    private val http = HttpClient.newHttpClient()

    companion object {
        @JvmStatic @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) {
            PasswordResetIntegrationTest.database(registry)
            registry.add("spring.datasource.hikari.maximum-pool-size") { 3 }
        }
    }

    @Test
    fun `report category stays separate from the status and decisions are validated and idempotent`() {
        val reporter = account()
        val admin = account("ADMIN")
        val hidden = db.fetchOne(
            "INSERT INTO report_category(report_type, is_selectable) VALUES ('Historical category', false) RETURNING id",
        )!!.get(0, Long::class.javaObjectType)!!
        val categories = body(request("GET", "/api/reports/categories", reporter), 200)
        assertFalse(categories.any { it["id"].asLong() == hidden })
        assertEquals(400, request("POST", "/api/reports", reporter, reportPayload(hidden, 1)).statusCode())
        val category = categories.first()["id"].asLong()
        val id = body(
            request("POST", "/api/reports", reporter, reportPayload(category, 1) + ("userId" to admin.id)),
            201,
        ).asLong()
        val path = "/api/reports/$id"
        val open = body(request("GET", path, admin), 200)
        assertEquals("OPEN", open["report"]["status"].asText())
        assertEquals(reporter.id, open["report"]["userId"].asLong())
        assertTrue(open["decisions"].isEmpty)
        assertTrue(body(request("GET", "/api/reports?status=OPEN", admin), 200).any { it["id"].asLong() == id })
        assertEquals(401, request("GET", path).statusCode())
        assertEquals(403, request("GET", "/api/reports", reporter).statusCode())
        assertEquals(403, request("GET", path, reporter).statusCode())
        assertEquals(403, decide(path, reporter, "RESOLVED", "Inspected").statusCode())
        assertEquals(410, request("PATCH", "$path/status?status=RESOLVED", admin).statusCode())
        assertEquals(400, decide(path, admin, "OPEN", "Inspected").statusCode())
        assertEquals(400, decide(path, admin, "OTHER", "Inspected").statusCode())
        assertEquals(400, decide(path, admin, 1, "Inspected").statusCode())
        assertEquals(400, decide(path, admin, "RESOLVED", "  ").statusCode())
        assertEquals(400, decide(path, admin, "RESOLVED", "x".repeat(501)).statusCode())

        val closed = body(decide(path, admin, "RESOLVED", "  Checked the event  "), 200)
        assertEquals("RESOLVED", closed["report"]["status"].asText())
        assertEquals(category, closed["report"]["categoryId"].asLong())
        assertEquals(categories.first()["name"].asText(), closed["report"]["categoryName"].asText())
        assertEquals("Report description", closed["report"]["description"].asText())
        val decision = closed["decisions"].single()
        assertEquals("Checked the event", decision["reason"].asText())
        assertEquals("RESOLVED", decision["outcome"].asText())
        assertEquals(admin.id, decision["moderatorId"].asLong())
        assertEquals(admin.username, decision["moderatorUsername"].asText())
        assertTrue(decision["decidedAt"].isTextual)
        assertTrue(decision["blockedUntil"].isNull)
        assertEquals(closed, body(decide(path, admin, "RESOLVED", "Checked the event"), 200))
        assertEquals(409, decide(path, admin, "RESOLVED", "Different reason").statusCode())
        assertEquals(closed, body(request("GET", path, admin), 200))
        assertFalse(body(request("GET", "/api/reports?status=OPEN", admin), 200).any { it["id"].asLong() == id })
        assertTrue(body(request("GET", "/api/reports?status=RESOLVED", admin), 200).any { it["id"].asLong() == id })
    }

    @Test
    fun `concurrent moderators produce one decision and the other gets a conflict`() {
        val reporter = account()
        val first = account("ADMIN")
        val second = account("ADMIN")
        val path = "/api/reports/${create(reporter)}"
        val responses = listOf(
            CompletableFuture.supplyAsync { decide(path, first, "RESOLVED", "Confirmed by first") },
            CompletableFuture.supplyAsync { decide(path, second, "DISMISSED", "Dismissed by second") },
        ).map { it.get(15, TimeUnit.SECONDS) }

        assertEquals(listOf(200, 409), responses.map { it.statusCode() }.sorted())
        val saved = body(request("GET", path, first), 200)
        assertEquals(body(responses.single { it.statusCode() == 200 }, 200), saved)
        assertEquals(saved["report"]["status"], saved["decisions"].single()["outcome"])
    }

    @Test
    fun `manual deletion protects decisions and event or account deletion handles their references`() {
        val reporter = account()
        val moderator = account("ADMIN")
        val admin = account("ADMIN")
        val open = "/api/reports/${create(reporter)}"
        assertEquals(204, request("DELETE", open, admin).statusCode())
        assertEquals(404, request("GET", open, admin).statusCode())
        assertEquals(404, decide(open, admin, "RESOLVED", "Already gone").statusCode())

        val dismissed = create(reporter)
        val path = "/api/reports/$dismissed"
        assertEquals("DISMISSED", body(decide(path, moderator, "DISMISSED", "No violation"), 200)["report"]["status"].asText())
        assertEquals(409, request("DELETE", path, admin).statusCode())
        assertEquals(204, request("DELETE", "/api/users/${moderator.id}", moderator).statusCode())
        val saved = body(request("GET", path, admin), 200)["decisions"].single()
        assertTrue(saved["moderatorId"].isNull)
        assertEquals("No violation", saved["reason"].asText())
        assertEquals(204, request("DELETE", "/api/users/${reporter.id}", reporter).statusCode())
        assertEquals(404, request("GET", path, admin).statusCode())
        assertEquals(0, decisionCount(dismissed))

        val event = db.fetchOne(
            """
            INSERT INTO event(name, description, location_id, start_at, created_at, user_id)
            SELECT 'Moderation test', '', id, now(), now(), ? FROM location ORDER BY id LIMIT 1
            RETURNING id
            """.trimIndent(), admin.id,
        )!!.get(0, Long::class.javaObjectType)!!
        val reportedEvent = create(admin, event)
        val eventReportPath = "/api/reports/$reportedEvent"
        body(decide(eventReportPath, admin, "RESOLVED", "Reviewed"), 200)
        assertEquals(204, request("DELETE", "/api/events/$event", admin).statusCode())
        assertEquals(404, request("GET", eventReportPath, admin).statusCode())
        assertEquals(0, decisionCount(reportedEvent))
    }

    private fun decisionCount(reportId: Long) =
        db.fetchOne("SELECT count(*) FROM decision WHERE report_id = ?", reportId)!!.get(0, Int::class.javaObjectType)

    private fun create(reporter: Account, eventId: Long = 1): Long {
        val category = body(request("GET", "/api/reports/categories", reporter), 200).first()["id"].asLong()
        return body(request("POST", "/api/reports", reporter, reportPayload(category, eventId)), 201).asLong()
    }

    private fun reportPayload(categoryId: Long, eventId: Long) =
        mapOf("categoryId" to categoryId, "eventId" to eventId, "description" to "Report description")

    private fun decide(path: String, admin: Account, status: Any, reason: String) =
        request("POST", "$path/decision", admin, mapOf("status" to status, "reason" to reason))

    private data class Account(val id: Long, val username: String, val token: String)

    private fun account(role: String = "USER"): Account {
        val key = UUID.randomUUID().toString().take(15)
        val email = "$key@example.com"
        val password = "moderation test password"
        db.execute(
            """
            INSERT INTO app_user(name,email,phone,birthdate,password_hash,username,page_description,role)
            VALUES ('Test', ?, ?, DATE '1990-01-01', ?, ?, '', ?)
            """.trimIndent(),
            email, key, passwords.encode(password), key, role,
        )
        val login = body(request("POST", "/api/auth/login", payload = mapOf("usernameOrEmail" to email, "password" to password)), 200)
        return Account(login["userId"].asLong(), key, login["accessToken"].asText())
    }

    private fun request(method: String, path: String, user: Account? = null, payload: Any? = null): HttpResponse<String> {
        val request = HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path"))
        user?.let { request.header("Authorization", "Bearer ${it.token}") }
        val publisher = if (payload == null) HttpRequest.BodyPublishers.noBody() else {
            request.header("Content-Type", "application/json")
            HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(payload))
        }
        return http.send(request.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString())
    }

    private fun body(response: HttpResponse<String>, status: Int): JsonNode {
        assertEquals(status, response.statusCode(), response.body())
        return json.readTree(response.body())
    }
}
