package com.evmap.mobileapp.feature.chat.data

import com.evmap.mobileapp.core.data.dto.MarkChatReadRequest
import com.evmap.mobileapp.core.data.dto.MessageDto
import com.evmap.mobileapp.core.data.dto.SendMessageRequest
import com.evmap.mobileapp.core.data.session.TokenStore
import com.squareup.moshi.Moshi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

sealed interface ChatRealtimeEvent {
    data class Message(val message: MessageDto) : ChatRealtimeEvent
}

@Singleton
class ChatRealtimeClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenStore: TokenStore,
    private val moshi: Moshi,
    @Named("baseUrl") private val baseUrl: String,
) {
    private val messageAdapter = moshi.adapter(MessageDto::class.java)
    private val sendMessageAdapter = moshi.adapter(SendMessageRequest::class.java)
    private val readAdapter = moshi.adapter(MarkChatReadRequest::class.java)
    private var webSocket: WebSocket? = null

    fun connect(chatId: Long): Flow<ChatRealtimeEvent> = callbackFlow {
        close()
        val request = Request.Builder()
            .url(baseUrl.toWebSocketUrl())
            .build()

        webSocket = okHttpClient.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(connectFrame())
                    webSocket.send(subscribeFrame("chat-$chatId-messages", "/topic/chats/$chatId/messages"))
                    webSocket.send(subscribeFrame("chat-$chatId-typing", "/topic/chats/$chatId/typing"))
                    webSocket.send(subscribeFrame("chat-$chatId-read", "/topic/chats/$chatId/read"))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    text.split(FRAME_TERMINATOR)
                        .filter { it.isNotBlank() }
                        .forEach { frame ->
                            val destination = frame.header("destination")
                            val body = frame.substringAfter("\n\n", "")
                            if (destination?.endsWith("/messages") == true && body.isNotBlank()) {
                                messageAdapter.fromJson(body)?.let { message ->
                                    trySend(ChatRealtimeEvent.Message(message))
                                }
                            }
                        }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    channel.close(t)
                }
            }
        )

        awaitClose {
            close()
        }
    }

    fun sendMessage(chatId: Long, text: String): Boolean =
        sendJson("/app/chats/$chatId/messages", sendMessageAdapter.toJson(SendMessageRequest(text)))

    fun sendRead(chatId: Long, lastReadMessageId: Long): Boolean =
        sendJson("/app/chats/$chatId/read", readAdapter.toJson(MarkChatReadRequest(lastReadMessageId)))

    fun close() {
        webSocket?.close(1000, null)
        webSocket = null
    }

    private fun connectFrame(): String {
        val token = tokenStore.accessToken()
        val authorization = if (token.isNullOrBlank()) "" else "Authorization:Bearer $token\n"
        return "CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n${authorization}\n$FRAME_TERMINATOR"
    }

    private fun subscribeFrame(id: String, destination: String): String =
        "SUBSCRIBE\nid:$id\ndestination:$destination\n\n$FRAME_TERMINATOR"

    private fun sendJson(destination: String, body: String): Boolean =
        webSocket?.send("SEND\ndestination:$destination\ncontent-type:application/json\n\n$body$FRAME_TERMINATOR") == true

    private fun String.header(name: String): String? =
        lineSequence()
            .takeWhile { it.isNotBlank() }
            .firstOrNull { it.startsWith("$name:") }
            ?.substringAfter(":")
            ?.trim()

    private fun String.toWebSocketUrl(): String {
        val base = trimEnd('/')
        val wsBase = when {
            base.startsWith("https://") -> base.replaceFirst("https://", "wss://")
            base.startsWith("http://") -> base.replaceFirst("http://", "ws://")
            else -> base
        }
        return "$wsBase/ws"
    }

    private companion object {
        const val FRAME_TERMINATOR = "\u0000"
    }
}
