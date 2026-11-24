package com.evmap.mobileapp.feature.chat

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToIndex
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
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

@RunWith(AndroidJUnit4::class)
class ChatNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun opensDirectChatAndRecoversHistoryAfterDisconnect() {
        val store = testTokenStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val previous = store.session()
        val proxy = listOf("http.proxyHost", "http.proxyPort").associateWith(System::getProperty)
        ChatFixture().use { fixture ->
            try {
                System.setProperty("http.proxyHost", "127.0.0.1")
                System.setProperty("http.proxyPort", fixture.port.toString())
                store.save(AuthResponse("fixture-access", "fixture-refresh", expiresInSeconds = 3600, userId = 1, username = "test"))
                ActivityScenario.launch(MainActivity::class.java).use {
                    compose.onNode(hasSetTextAction()).performTextReplacement("@alice")
                    compose.onNode(hasSetTextAction()).performImeAction()
                    awaitText("alice")
                    compose.onNodeWithText("alice").performClick()
                    awaitText("Message")
                    compose.onNodeWithText("Message").performClick()
                    awaitText("History 100")
                    assertEquals(listOf(2), fixture.openedPeers)
                    compose.waitUntil(15_000) { 100 in fixture.readIds }

                    fixture.typing()
                    awaitText("alice is typing…")
                    fixture.disconnect()
                    awaitText("Missed while offline")
                    compose.waitUntil(15_000) { 101 in fixture.readIds }

                    compose.onNode(hasSetTextAction()).performTextReplacement("See you there")
                    compose.onNodeWithContentDescription("Send").performClick()
                    awaitText("Retry send")
                    compose.onNodeWithText("Retry send").performClick()
                    awaitText("Sent")
                    assertEquals(2, fixture.sendKeys.size)
                    assertEquals(fixture.sendKeys.first(), fixture.sendKeys.last())
                    fixture.peerRead()
                    awaitText("Read")

                    compose.onNodeWithTag("chat_history").performScrollToIndex(53)
                    compose.waitUntil(15_000) { 51 in fixture.olderAnchors }
                    compose.onNodeWithTag("chat_history").performScrollToIndex(53)
                    awaitText("Older message")

                    compose.onNodeWithContentDescription("Attach file").performClick()
                    awaitText("Attach to chat")
                    compose.onNodeWithText("Photo").assertExists()
                    compose.onNodeWithText("File").assertExists()
                }
            } finally {
                proxy.forEach { (name, value) -> if (value == null) System.clearProperty(name) else System.setProperty(name, value) }
                if (previous == null) store.clear() else store.save(AuthResponse(
                    previous.accessToken, previous.refreshToken, previous.tokenType,
                    (previous.expiresAtMillis - System.currentTimeMillis()) / 1000,
                    previous.userId, previous.username,
                ))
            }
        }
    }

    private fun awaitText(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private class ChatFixture : Closeable {
        private val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        val port = server.localPort
        val openedPeers = CopyOnWriteArrayList<Int>()
        val sendKeys = CopyOnWriteArrayList<String>()
        val readIds = CopyOnWriteArrayList<Int>()
        val olderAnchors = CopyOnWriteArrayList<Int>()
        @Volatile private var socket: Socket? = null
        @Volatile private var missed = false
        private val chat = """{"id":7,"name":"alice","usersCount":2,"lastMessageAt":null,"userIds":[1,2]}"""
        private val user = """{"id":2,"name":"Alice","email":"alice@example.com","phone":"+48123456781","birthdate":"1990-01-01","username":"alice","pageDescription":"","followersCount":0,"followingCount":0}"""

        init {
            thread(isDaemon = true, name = "Chat fixture") {
                while (!server.isClosed) {
                    val connection = runCatching { server.accept() }.getOrNull() ?: break
                    thread(isDaemon = true) { runCatching { connection.use(::serve) } }
                }
            }
        }

        fun typing() {
            sendEvent(checkNotNull(socket), "typing", """{"chatId":7,"userId":2,"username":"alice","typing":true}""")
        }

        fun peerRead() {
            sendEvent(checkNotNull(socket), "read", """{"chatId":7,"userId":2,"lastReadMessageId":102,"lastReadSentAt":"2030-01-01T12:00:00Z"}""")
        }

        fun disconnect() {
            missed = true
            checkNotNull(socket).close()
        }

        private fun serve(connection: Socket) {
            connection.soTimeout = 5_000
            val input = DataInputStream(BufferedInputStream(connection.getInputStream()))
            val (method, target) = input.readHttpLine().split(' ')
            val url = (if (target.startsWith("/")) BuildConfig.BASE_URL.trimEnd('/') + target else target).toHttpUrl()
            val headers = generateSequence { input.readHttpLine().takeIf(String::isNotEmpty) }
                .associate { it.substringBefore(':').lowercase() to it.substringAfter(':').trim() }
            if (url.encodedPath == "/ws") {
                serveSocket(connection, input, checkNotNull(headers["sec-websocket-key"]))
                return
            }
            val length = headers["content-length"]?.toInt() ?: 0
            val body = ByteArray(length).also(input::readFully).decodeToString()
            val (status, payload) = respond(method, url, body)
            val bytes = payload.encodeToByteArray()
            connection.getOutputStream().apply {
                write("HTTP/1.1 $status Fixture\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".encodeToByteArray())
                write(bytes)
                flush()
            }
        }

        private fun serveSocket(connection: Socket, input: DataInputStream, key: String) {
            connection.soTimeout = 0
            val digest = MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").encodeToByteArray())
            val accept = Base64.getEncoder().encodeToString(digest)
            connection.getOutputStream().apply {
                write(("HTTP/1.1 101 Switching Protocols\r\nConnection: Upgrade\r\nUpgrade: websocket\r\n" +
                    "Sec-WebSocket-Accept: $accept\r\nSec-WebSocket-Protocol: v12.stomp\r\n\r\n").encodeToByteArray())
                flush()
            }
            while (!connection.isClosed) {
                val opcode = input.readUnsignedByte() and 15
                val flags = input.readUnsignedByte()
                val length = when (val size = flags and 127) {
                    126 -> input.readUnsignedShort()
                    127 -> input.readLong().toInt()
                    else -> size
                }
                val mask = ByteArray(4).also(input::readFully)
                val bytes = ByteArray(length).also(input::readFully)
                bytes.indices.forEach { i -> bytes[i] = (bytes[i].toInt() xor mask[i % 4].toInt()).toByte() }
                if (opcode == 8) return
                if (opcode == 9) writeFrame(connection, 10, bytes)
                val stomp = bytes.decodeToString()
                when {
                    stomp.startsWith("CONNECT\n") -> writeFrame(connection, 1, "CONNECTED\nversion:1.2\n\n\u0000".encodeToByteArray())
                    stomp.startsWith("SEND\n") && stomp.contains("\"typing\":false") -> {
                        socket = connection
                        sendEvent(connection, "typing", """{"chatId":7,"userId":1,"username":"test","typing":false}""")
                    }
                }
            }
        }

        private fun sendEvent(connection: Socket, topic: String, json: String) =
            writeFrame(connection, 1, "MESSAGE\ndestination:/topic/chats/7/$topic\n\n$json\u0000".encodeToByteArray())

        private fun writeFrame(connection: Socket, opcode: Int, bytes: ByteArray) {
            synchronized(connection) {
                connection.getOutputStream().apply {
                    write(128 or opcode)
                    if (bytes.size < 126) {
                        write(bytes.size)
                    } else {
                        write(126)
                        write(bytes.size shr 8)
                        write(bytes.size and 255)
                    }
                    write(bytes)
                    flush()
                }
            }
        }

        private fun respond(method: String, url: HttpUrl, body: String): Pair<Int, String> = when (url.encodedPath) {
            "/api/events/feed", "/api/events", "/api/users/2/events" -> 200 to """{"content":[],"number":0,"size":20,"totalElements":0,"totalPages":0,"first":true,"last":true}"""
            "/api/events/categories", "/api/users/1/following" -> 200 to "[]"
            "/api/search" -> 200 to """{"type":"users","limit":30,"users":[{"id":2,"username":"alice","name":"Alice","pageDescription":""}]}"""
            "/api/users/2" -> 200 to user
            "/api/chats/direct/2" -> {
                check(method == "POST")
                openedPeers += 2
                200 to "7"
            }
            "/api/chats/7" -> 200 to chat
            "/api/chats/7/messages" -> when {
                method == "POST" -> {
                    val request = JSONObject(body)
                    sendKeys += request.getString("clientMessageId")
                    if (sendKeys.size == 1) 503 to "{}" else 200 to message(102, request.getString("text"), sender = 1)
                }
                url.queryParameter("beforeId") != null -> {
                    olderAnchors += checkNotNull(url.queryParameter("beforeId")).toInt()
                    200 to "[${message(49, "Older message")},${message(50, "History 50")}]"
                }
                url.queryParameter("afterId") != null ->
                    200 to if (missed && url.queryParameter("afterId") == "100") "[${message(101, "Missed while offline")}]" else "[]"
                else -> 200 to (51..100).joinToString(prefix = "[", postfix = "]") { message(it, "History $it") }
            }
            "/api/chats/7/read" -> if (method == "POST") {
                readIds += JSONObject(body).getInt("lastReadMessageId")
                204 to ""
            } else 200 to "[]"
            else -> 404 to "{}"
        }

        private fun message(id: Int, text: String, sender: Int = 2) = JSONObject()
            .put("id", id).put("chatId", 7).put("senderId", sender)
            .put("senderUsername", if (sender == 1) "test" else "alice").put("text", text)
            .put("sentAt", "2030-01-01T12:00:00Z").toString()

        private fun DataInputStream.readHttpLine(): String = buildString {
            while (true) {
                val byte = readUnsignedByte()
                if (byte == 10) break
                if (byte != 13) append(byte.toChar())
            }
        }

        override fun close() {
            socket?.close()
            server.close()
        }
    }
}
