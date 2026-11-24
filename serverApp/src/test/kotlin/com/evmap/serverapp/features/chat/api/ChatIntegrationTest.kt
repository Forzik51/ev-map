package com.evmap.serverapp.features.chat.api

import com.evmap.serverapp.features.auth.PasswordResetIntegrationTest
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

@EnabledIfEnvironmentVariable(named = "EVMAP_TEST_DB_URL", matches = ".+")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatIntegrationTest {
    @LocalServerPort private var port = 0
    @Autowired private lateinit var db: DSLContext
    @Autowired private lateinit var json: ObjectMapper
    @Autowired private lateinit var passwords: PasswordEncoder
    private val http = HttpClient.newHttpClient()

    companion object {
        @JvmStatic @DynamicPropertySource
        fun database(registry: DynamicPropertyRegistry) = PasswordResetIntegrationTest.database(registry)
    }

    @Test
    fun `both members open the same direct chat and cursor pages cover tied timestamps`() {
        val first = account()
        val second = account()
        val outsider = account()
        val one = CompletableFuture.supplyAsync { direct(first, second) }
        val two = CompletableFuture.supplyAsync { direct(second, first) }
        val id = one.get(15, TimeUnit.SECONDS)
        assertEquals(id, two.get(15, TimeUnit.SECONDS))
        val path = "/api/chats/$id"
        assertEquals(second.username, json(request("GET", path, first), 200)["name"].asText())
        assertEquals(403, request("GET", path, outsider).statusCode())
        assertEquals(403, request("GET", "$path/search?query=Test", outsider).statusCode())

        db.execute("""
            INSERT INTO message(text, sent_at, chat_id, user_id)
            SELECT 'Test ' || number, TIMESTAMPTZ '2025-01-01 00:00:00Z', ?, ? FROM generate_series(1, 120) number
        """.trimIndent(), id, first.id)
        fun ids(suffix: String) = json(request("GET", "$path/messages?limit=50$suffix", second), 200).map { it["id"].asLong() }
        val latest = ids("")
        val middle = ids("&beforeId=${latest.first()}")
        val oldest = ids("&beforeId=${middle.first()}")
        assertEquals(listOf(20, 50, 50), listOf(oldest.size, middle.size, latest.size))
        val combined = oldest + middle + latest
        assertEquals(combined.sorted().distinct(), combined)
        assertEquals(combined.drop(1).take(50), ids("&afterId=${oldest.first()}"))
    }

    @Test
    fun `REST publishes saved messages and monotonic reads over STOMP and attachments are private`() {
        val sender = account()
        val recipient = account()
        val outsider = account()
        val chat = direct(sender, recipient)
        val path = "/api/chats/$chat"
        val listener = Listener()
        val socket = http.newWebSocketBuilder().subprotocols("v12.stomp")
            .buildAsync(URI("ws://127.0.0.1:$port/ws"), listener).join()
        try {
            socket.sendText("CONNECT\naccept-version:1.2\nAuthorization:Bearer ${recipient.token}\n\n\u0000", true).join()
            listener.await("CONNECTED")
            for (topic in listOf("messages", "read", "typing")) {
                socket.sendText("SUBSCRIBE\nid:$topic\ndestination:/topic/chats/$chat/$topic\n\n\u0000", true).join()
            }
            socket.sendText("SEND\ndestination:/app/chats/$chat/typing\ncontent-type:application/json\n\n{\"typing\":false}\u0000", true).join()
            listener.await("/typing")

            val key = UUID.randomUUID()
            val payload = mapOf("text" to "Hello", "clientMessageId" to key)
            val first = json(request("POST", "$path/messages", sender, payload), 201)["id"].asLong()
            assertTrue(listener.await("/messages").contains("Hello"))
            fun summary(viewer: Account) = json(request("GET", "/api/chats", viewer), 200).single { it["id"].asLong() == chat }
            assertEquals("Hello", summary(recipient)["lastMessageText"].asText())
            assertEquals(1, summary(recipient)["unreadCount"].asInt())
            assertEquals(0, summary(sender)["unreadCount"].asInt())
            assertEquals(first, json(request("POST", "$path/messages", sender, payload), 201)["id"].asLong())
            assertEquals(1, json(request("GET", "$path/messages", recipient), 200).size())

            val file = "private file".encodeToByteArray()
            val attachmentKey = UUID.randomUUID()
            val attached = json(upload("$path/attachments?kind=FILE&clientMessageId=$attachmentKey", sender, file), 201)
            val latest = attached["id"].asLong()
            listener.await("/messages")
            assertEquals(latest, json(upload("$path/attachments?kind=FILE&clientMessageId=$attachmentKey", sender, file), 201)["id"].asLong())
            val url = "$path/attachments/${attached["attachment"]["id"].asLong()}"
            assertArrayEquals(file, request("GET", url, recipient).body())
            assertEquals(403, request("GET", url, outsider).statusCode())
            assertEquals(403, upload("$path/attachments?kind=FILE&clientMessageId=${UUID.randomUUID()}", outsider, file).statusCode())
            assertEquals("FILE", summary(recipient)["lastAttachmentKind"].asText())
            assertEquals(2, summary(recipient)["unreadCount"].asInt())

            for (id in listOf(latest, first)) {
                assertEquals(204, request("POST", "$path/read", recipient, mapOf("lastReadMessageId" to id)).statusCode())
            }
            assertTrue(listener.await("/read").contains("\"lastReadMessageId\":$latest"))
            assertEquals(0, summary(recipient)["unreadCount"].asInt())
            assertEquals(latest, json(request("GET", "$path/read", sender), 200).single()["lastReadMessageId"].asLong())
            assertEquals(204, request("DELETE", path, sender).statusCode())
        } finally {
            socket.abort()
        }
    }

    @Test
    fun `opening a direct chat does not prevent account deletion`() {
        val creator = account()
        val peer = account()
        val chatId = direct(creator, peer)
        val chatPath = "/api/chats/$chatId"
        val message = json(request("POST", "$chatPath/messages", creator, mapOf("text" to "Hello")), 201)["id"].asLong()
        assertEquals(204, request("POST", "$chatPath/read", peer, mapOf("lastReadMessageId" to message)).statusCode())
        assertEquals(204, request("DELETE", "/api/users/${creator.id}", creator).statusCode())
        assertEquals(0, json(request("GET", "$chatPath/messages", peer), 200).size())
        assertEquals(0, json(request("GET", "$chatPath/read", peer), 200).size())
    }

    private data class Account(val id: Long, val username: String, val token: String)

    private fun account(): Account {
        val key = UUID.randomUUID().toString().take(15)
        val email = "$key@example.com"
        val password = "chat test password"
        db.execute("""
            INSERT INTO app_user(name,email,phone,birthdate,password_hash,username,page_description)
            VALUES ('Test', ?, ?, DATE '1990-01-01', ?, ?, '')
        """.trimIndent(), email, key, passwords.encode(password), key)
        val response = json(request("POST", "/api/auth/login", payload = mapOf("usernameOrEmail" to email, "password" to password)), 200)
        return Account(response["userId"].asLong(), key, response["accessToken"].asText())
    }

    private fun direct(user: Account, peer: Account) = json(request("POST", "/api/chats/direct/${peer.id}", user), 200).asLong()

    private fun request(method: String, path: String, user: Account? = null, payload: Any? = null): HttpResponse<ByteArray> {
        val request = HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path"))
        user?.let { request.header("Authorization", "Bearer ${it.token}") }
        val body = if (payload == null) HttpRequest.BodyPublishers.noBody() else {
            request.header("Content-Type", "application/json")
            HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(payload))
        }
        return http.send(request.method(method, body).build(), HttpResponse.BodyHandlers.ofByteArray())
    }

    private fun upload(path: String, user: Account, bytes: ByteArray): HttpResponse<ByteArray> {
        val boundary = UUID.randomUUID().toString()
        val head = "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"file.txt\"\r\nContent-Type: text/plain\r\n\r\n".encodeToByteArray()
        val body = head + bytes + "\r\n--$boundary--\r\n".encodeToByteArray()
        val request = HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path"))
            .header("Authorization", "Bearer ${user.token}").header("Content-Type", "multipart/form-data; boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build()
        return http.send(request, HttpResponse.BodyHandlers.ofByteArray())
    }

    private fun json(response: HttpResponse<ByteArray>, status: Int): JsonNode {
        assertEquals(status, response.statusCode(), response.body().decodeToString())
        return json.readTree(response.body())
    }

    private class Listener : WebSocket.Listener {
        private val frames = LinkedBlockingQueue<String>()
        private val pending = StringBuilder()
        override fun onOpen(webSocket: WebSocket) { webSocket.request(1) }
        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletableFuture<*>? {
            pending.append(data)
            if (last) {
                frames.add(pending.toString())
                pending.clear()
            }
            webSocket.request(1)
            return null
        }
        fun await(pattern: String): String {
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (System.nanoTime() < deadline) {
                val frame = frames.poll((deadline - System.nanoTime()).coerceAtLeast(0), TimeUnit.NANOSECONDS) ?: break
                if (frame.contains(pattern)) return frame
                check(!frame.startsWith("ERROR")) { frame }
            }
            error("STOMP frame did not arrive: $pattern")
        }
    }
}
