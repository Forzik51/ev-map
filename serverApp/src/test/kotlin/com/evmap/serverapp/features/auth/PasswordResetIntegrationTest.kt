package com.evmap.serverapp.features.auth

import com.evmap.serverapp.features.auth.infra.PasswordResetMailWorker
import com.evmap.serverapp.shared.email.application.EmailSenderPort
import com.evmap.serverapp.shared.email.domain.EmailMessage
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.mail.MailSendException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

// Opt in only with a disposable database. No test connects to the application's evmap database.
@EnabledIfEnvironmentVariable(named = "EVMAP_TEST_DB_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "spring.flyway.baseline-version=0",
    "app.security.password-reset.mail-poll-ms=86400000",
    "app.security.password-reset.cleanup-ms=86400000",
    "app.security.password-reset.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
])
@Import(PasswordResetIntegrationTest.TimeConfig::class)
class PasswordResetIntegrationTest {
    @LocalServerPort private var port = 0
    @Autowired private lateinit var dsl: DSLContext
    @Autowired private lateinit var json: ObjectMapper
    @Autowired private lateinit var encoder: PasswordEncoder
    @Autowired private lateinit var clock: TestClock
    @Autowired private lateinit var worker: PasswordResetMailWorker
    @Autowired private lateinit var mail: RecordingEmailSender
    private val client = HttpClient.newHttpClient()
    private val letters get() = mail.letters
    private var failMail: Boolean
        get() = mail.fail
        set(value) { mail.fail = value }
    private lateinit var email: String
    private var userId = 0L
    private val oldPassword = "original password"

    companion object {
        @JvmStatic @DynamicPropertySource
        fun database(properties: DynamicPropertyRegistry) {
            val url = requireNotNull(System.getenv("EVMAP_TEST_DB_URL"))
            require(Regex("^jdbc:postgresql://(127\\.0\\.0\\.1|localhost):[0-9]+/evmap_password_reset_test(\\?.*)?$").matches(url)) {
                "Recovery integration tests require the isolated local evmap_password_reset_test database"
            }
            properties.add("spring.datasource.url") { url }
            properties.add("spring.datasource.username") { System.getenv("EVMAP_TEST_DB_USER") ?: "reset_test" }
            properties.add("spring.datasource.password") { System.getenv("EVMAP_TEST_DB_PASSWORD") ?: "" }
            properties.add("spring.datasource.hikari.maximum-pool-size") { 5 }
        }
    }

    @BeforeEach fun setup() {
        clock.now = Instant.now()
        dsl.execute("TRUNCATE password_reset_mail_outbox, password_reset_request")
        letters.clear()
        failMail = false
        email = "${UUID.randomUUID()}@example.com"
        userId = createUser(email)
    }

    @Test fun `reset replaces password and immediately invalidates access and refresh tokens`() {
        val login = login(oldPassword)
        assertEquals(200, login.statusCode())
        val access = body(login)["accessToken"].asText()
        val refresh = body(login)["refreshToken"].asText()
        val requestId = requestCode()
        val token = verify(requestId, deliveredCode())
        assertEquals(204, reset(token).statusCode())
        assertEquals(401, login(oldPassword).statusCode())
        val newLogin = login("replacement password")
        assertEquals(200, newLogin.statusCode())
        assertEquals(401, get("/api/users/me", access).statusCode())
        assertEquals(200, get("/api/users/me", body(newLogin)["accessToken"].asText()).statusCode())
        assertEquals(401, post("/api/auth/refresh", mapOf("refreshToken" to refresh)).statusCode())
        assertEquals(400, reset(token).statusCode())
        assertEquals(1L, dsl.fetchOne("SELECT auth_version FROM app_user WHERE id = ?", userId)!!.get(0, Long::class.java))
        worker.deliverPending()
        assertEquals(2, letters.size)
        assertFalse(letters.last().body.contains("replacement password"))
    }

    @Test fun `unknown email gets identical accepted response without a message`() {
        // PostgreSQL rounds nanoseconds to microseconds; zero cooldown must stay disabled.
        clock.now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).plusNanos(999_999_999)
        val response = post("/api/auth/password/forgot", mapOf("email" to "missing-${UUID.randomUUID()}@example.com"))
        assertEquals(202, response.statusCode())
        val unknown = body(response)
        val knownResponse = post("/api/auth/password/forgot", mapOf("email" to email.uppercase()))
        assertEquals(202, knownResponse.statusCode())
        val known = body(knownResponse)
        assertEquals(known["message"], unknown["message"])
        assertEquals(known["resendAfterSeconds"], unknown["resendAfterSeconds"])
        assertNotEquals(known["requestId"], unknown["requestId"])
        worker.deliverPending()
        assertEquals(1, letters.size)
        assertEquals(email, letters.single().to)
    }

    @Test fun `failed guesses persist and exhaust code without locking login`() {
        val id = requestCode()
        val code = deliveredCode()
        val wrong = if (code == "00000000") "11111111" else "00000000"
        repeat(5) { assertEquals(400, verifyResponse(id, wrong).statusCode()) }
        assertEquals(5, dsl.fetchOne("SELECT failed_attempts FROM password_reset_request WHERE id = ?", UUID.fromString(id))!!.get(0, Int::class.java))
        assertEquals(400, verifyResponse(id, code).statusCode())
        assertEquals(200, login(oldPassword).statusCode())
    }

    @Test fun `expired code is rejected`() {
        val id = requestCode(); val code = deliveredCode()
        clock.advance(601)
        assertEquals(400, verifyResponse(id, code).statusCode())
    }

    @Test fun `code can only be exchanged once and grant expires`() {
        val id = requestCode(); val code = deliveredCode()
        val token = verify(id, code)
        assertEquals(400, verifyResponse(id, code).statusCode())
        clock.advance(301)
        val expired = reset(token)
        assertEquals(400, expired.statusCode())
        assertEquals("INVALID_OR_EXPIRED_RESET_TOKEN", body(expired)["code"].asText())
        assertEquals(200, login(oldPassword).statusCode())
    }

    @Test fun `resend enforces cooldown and invalidates previous request`() {
        val first = requestCode(); val oldCode = deliveredCode()
        val limited = post("/api/auth/password/forgot", mapOf("email" to email))
        assertEquals(429, limited.statusCode())
        assertTrue(limited.headers().firstValue("Retry-After").get().toLong() > 0)
        clock.advance(61)
        val second = requestCode(); val newCode = deliveredCode()
        assertEquals(400, verifyResponse(first, oldCode).statusCode())
        assertEquals(200, verifyResponse(second, newCode).statusCode())
    }

    @Test fun `email and IP hourly limits apply to unknown accounts too`() {
        repeat(5) {
            assertEquals(202, post("/api/auth/password/forgot", mapOf("email" to "unknown@example.com")).statusCode())
            clock.advance(61)
        }
        assertEquals(429, post("/api/auth/password/forgot", mapOf("email" to "unknown@example.com")).statusCode())
        repeat(15) {
            assertEquals(202, post("/api/auth/password/forgot", mapOf("email" to "unknown-$it@example.com")).statusCode())
        }
        assertEquals(429, post("/api/auth/password/forgot", mapOf("email" to "over-ip-limit@example.com")).statusCode())
    }

    @Test fun `concurrent verification yields only one grant`() {
        val id = requestCode(); val code = deliveredCode()
        assertEquals(listOf(200, 400), concurrently { verifyResponse(id, code).statusCode() }.sorted())
    }

    @Test fun `concurrent reset consumes the grant exactly once`() {
        val id = requestCode(); val token = verify(id, deliveredCode())
        assertEquals(listOf(204, 400), concurrently { reset(token).statusCode() }.sorted())
        assertEquals(1L, dsl.fetchOne("SELECT auth_version FROM app_user WHERE id = ?", userId)!!.get(0, Long::class.java))
    }

    @Test fun `SMTP failure remains queued and retries without exposing code in database`() {
        val id = requestCode()
        failMail = true; worker.deliverPending()
        assertTrue(letters.isEmpty())
        assertEquals(1, dsl.fetchOne("SELECT attempts FROM password_reset_mail_outbox WHERE request_id = ?", UUID.fromString(id))!!.get(0, Int::class.java))
        failMail = false; clock.advance(31); worker.deliverPending()
        assertEquals(1, letters.size)
        val code = Regex("[0-9]{8}").find(letters.single().body)!!.value
        val encrypted = dsl.fetchOne("SELECT encrypted_body FROM password_reset_mail_outbox WHERE request_id = ?", UUID.fromString(id))!!.get(0, String::class.java)!!
        assertFalse(encrypted.contains(code))
        assertEquals(200, verifyResponse(id, code).statusCode())
    }

    @Test fun `invalid payload and oversized UTF8 passwords leave credentials unchanged`() {
        assertEquals(400, post("/api/auth/password/forgot", mapOf("email" to "not-an-email")).statusCode())
        val id = requestCode(); val token = verify(id, deliveredCode())
        assertEquals(400, reset(token, "short").statusCode())
        assertEquals(400, reset(token, "я".repeat(37)).statusCode())
        assertEquals(200, login(oldPassword).statusCode())
        assertEquals(204, reset(token).statusCode())
    }

    @Test fun `delivery stops after max attempts and expired queued codes are cancelled`() {
        requestCode(); failMail = true
        repeat(3) { worker.deliverPending(); clock.advance(120) }
        assertEquals(3, dsl.fetchOne("SELECT attempts FROM password_reset_mail_outbox")!!.get(0, Int::class.java))
        assertNotNull(dsl.fetchOne("SELECT failed_at FROM password_reset_mail_outbox")!!.get(0))
        failMail = false; worker.deliverPending(); assertTrue(letters.isEmpty())
        requestCode(); clock.advance(601); worker.deliverPending()
        assertTrue(letters.isEmpty())
    }

    @Test fun `resend invalidates an already verified grant`() {
        val id = requestCode(); val token = verify(id, deliveredCode())
        clock.advance(61); requestCode()
        assertEquals(400, reset(token).statusCode())
        assertEquals(200, login(oldPassword).statusCode())
    }

    @Test fun `password reset closes an established authenticated WebSocket`() {
        val access = body(login(oldPassword))["accessToken"].asText()
        val listener = SocketListener()
        val socket = connect(access, listener)
        try {
            assertTrue(listener.messages.poll(5, TimeUnit.SECONDS)?.startsWith("CONNECTED") == true)
            val id = requestCode(); val token = verify(id, deliveredCode())
            assertEquals(204, reset(token).statusCode())
            assertEquals(1008, listener.closed.get(5, TimeUnit.SECONDS))
        } finally { socket.abort() }
    }

    @Test fun `WebSocket refuses a subscription to another users chat`() {
        val other = createUser("${UUID.randomUUID()}@example.com")
        val chat = dsl.fetchOne("INSERT INTO chat(name, created_by_user_id) VALUES ('private', ?) RETURNING id", other)!!.get(0, Long::class.java)!!
        dsl.execute("INSERT INTO user_chat(chat_id,user_id,joined_at) VALUES (?,?,now())", chat, other)
        val access = body(login(oldPassword))["accessToken"].asText()
        val listener = SocketListener(); val socket = connect(access, listener)
        try {
            assertTrue(listener.messages.poll(5, TimeUnit.SECONDS)?.startsWith("CONNECTED") == true)
            socket.sendText("SUBSCRIBE\nid:foreign\ndestination:/topic/chats/$chat/messages\n\n\u0000", true).join()
            assertEquals(1008, listener.closed.get(5, TimeUnit.SECONDS))
        } finally { socket.abort() }
    }

    private fun createUser(address: String): Long {
        val key = UUID.randomUUID().toString().take(15)
        return dsl.fetchOne(
            """INSERT INTO app_user(name,email,phone,birthdate,password_hash,username,page_description)
               VALUES ('Test',?,?,DATE '1990-01-01',?,?,'Recovery test') RETURNING id""",
            address, key, encoder.encode(oldPassword), "test-$key",
        )!!.get(0, Long::class.java)!!
    }

    private fun requestCode(): String {
        val response = post("/api/auth/password/forgot", mapOf("email" to email))
        assertEquals(202, response.statusCode())
        return body(response)["requestId"].asText()
    }
    private fun deliveredCode(): String {
        worker.deliverPending()
        return Regex("[0-9]{8}").find(letters.last().body)!!.value
    }
    private fun verifyResponse(id: String, code: String) = post("/api/auth/password/verify", mapOf("requestId" to id, "code" to code))
    private fun verify(id: String, code: String): String {
        val response = verifyResponse(id, code); assertEquals(200, response.statusCode())
        return body(response)["resetToken"].asText()
    }
    private fun reset(token: String, password: String = "replacement password") =
        post("/api/auth/password/reset", mapOf("resetToken" to token, "newPassword" to password))
    private fun login(password: String) = post("/api/auth/login", mapOf("usernameOrEmail" to email, "password" to password))
    private fun post(path: String, data: Any): HttpResponse<String> = client.send(
        HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path")).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data))).build(), HttpResponse.BodyHandlers.ofString(),
    )
    private fun get(path: String, token: String): HttpResponse<String> = client.send(
        HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path")).header("Authorization", "Bearer $token").GET().build(),
        HttpResponse.BodyHandlers.ofString(),
    )
    private fun body(response: HttpResponse<String>): JsonNode = json.readTree(response.body())

    private fun concurrently(block: () -> Int): List<Int> {
        val executor = Executors.newFixedThreadPool(2)
        val start = CountDownLatch(1)
        return try {
            val futures = (1..2).map { executor.submit<Int> { start.await(); block() } }
            start.countDown(); futures.map { it.get(15, TimeUnit.SECONDS) }
        } finally { executor.shutdownNow() }
    }

    private fun connect(token: String, listener: SocketListener): WebSocket = client.newWebSocketBuilder()
        .buildAsync(URI("ws://127.0.0.1:$port/ws"), listener).get(5, TimeUnit.SECONDS).also {
            it.sendText("CONNECT\naccept-version:1.2\nhost:localhost\nAuthorization:Bearer $token\n\n\u0000", true).join()
        }

    private class SocketListener : WebSocket.Listener {
        val messages = LinkedBlockingQueue<String>()
        val closed = CompletableFuture<Int>()
        private val frame = StringBuilder()
        override fun onOpen(webSocket: WebSocket) { webSocket.request(1) }
        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletableFuture<*>? {
            frame.append(data)
            if (last) { messages.add(frame.toString()); frame.setLength(0) }
            webSocket.request(1)
            return null
        }
        override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletableFuture<*>? {
            closed.complete(statusCode); return null
        }
        override fun onError(webSocket: WebSocket, error: Throwable) { closed.completeExceptionally(error) }
    }

    class TestClock : Clock() {
        @Volatile var now: Instant = Instant.now()
        override fun instant(): Instant = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        fun advance(seconds: Long) { now = now.plusSeconds(seconds) }
    }
    @TestConfiguration class TimeConfig {
        @Bean @Primary fun testClock() = TestClock()
        @Bean @Primary fun recordingEmailSender() = RecordingEmailSender()
    }
    class RecordingEmailSender : EmailSenderPort {
        val letters = CopyOnWriteArrayList<EmailMessage>()
        @Volatile var fail = false
        override fun send(message: EmailMessage) {
            if (fail) throw MailSendException("Simulated delivery failure")
            letters.add(message)
        }
    }
}
