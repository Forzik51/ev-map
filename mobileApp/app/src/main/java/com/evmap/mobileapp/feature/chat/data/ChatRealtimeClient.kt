package com.evmap.mobileapp.feature.chat.data

import com.evmap.mobileapp.core.data.dto.ChatReadDto
import com.evmap.mobileapp.core.data.dto.ChatTypingDto
import com.evmap.mobileapp.core.data.dto.MessageDto
import com.evmap.mobileapp.core.data.session.TokenStore
import com.squareup.moshi.Moshi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

sealed interface ChatRealtimeEvent {
    data object Ready : ChatRealtimeEvent
    data class Message(val message: MessageDto) : ChatRealtimeEvent
    data class Typing(val event: ChatTypingDto) : ChatRealtimeEvent
    data class Read(val event: ChatReadDto) : ChatRealtimeEvent
}

@Singleton
class ChatRealtimeClient @Inject constructor(
    okHttpClient: OkHttpClient,
    private val tokenStore: TokenStore,
    moshi: Moshi,
    @Named("baseUrl") baseUrl: String,
) {
    private val client = okHttpClient.newBuilder().pingInterval(20, TimeUnit.SECONDS).build()
    private val url = baseUrl.trimEnd('/') + "/ws"
    private val messageAdapter = moshi.adapter(MessageDto::class.java)
    private val typingAdapter = moshi.adapter(ChatTypingDto::class.java)
    private val readAdapter = moshi.adapter(ChatReadDto::class.java)

    fun connect(chatId: Long, typing: Flow<Boolean>): Flow<ChatRealtimeEvent> = callbackFlow {
        val ready = AtomicBoolean(false)
        val pending = StringBuilder()
        val ownId = tokenStore.session()?.userId
        val request = Request.Builder().url(url).header("Sec-WebSocket-Protocol", "v12.stomp").build()
        val socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val token = tokenStore.accessToken()
                if (token.isNullOrBlank()) {
                    channel.close(IOException("Sign in again to continue."))
                    return
                }
                webSocket.send("CONNECT\naccept-version:1.2\nheart-beat:0,0\nAuthorization:Bearer $token\n\n$END")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                pending.append(text)
                while (true) {
                    val end = pending.indexOf(END)
                    if (end < 0) break
                    val frame = pending.substring(0, end).trimStart('\n', '\r').replace("\r\n", "\n")
                    pending.delete(0, end + END.length)
                    try {
                        when (frame.substringBefore('\n')) {
                            "CONNECTED" -> {
                                for (topic in listOf("messages", "typing", "read")) {
                                    webSocket.send("SUBSCRIBE\nid:$topic\ndestination:/topic/chats/$chatId/$topic\n\n$END")
                                }
                                // The server processes frames in receive order. Our echo confirms all subscriptions.
                                webSocket.sendTyping(chatId, false)
                            }
                            "MESSAGE" -> {
                                val destination = frame.substringBefore("\n\n").lineSequence()
                                    .firstOrNull { it.startsWith("destination:") }?.substringAfter(':')
                                val body = frame.substringAfter("\n\n", "")
                                when (destination) {
                                    "/topic/chats/$chatId/messages" -> messageAdapter.fromJson(body)?.let { emit(ChatRealtimeEvent.Message(it)) }
                                    "/topic/chats/$chatId/read" -> readAdapter.fromJson(body)?.let { emit(ChatRealtimeEvent.Read(it)) }
                                    "/topic/chats/$chatId/typing" -> typingAdapter.fromJson(body)?.let { event ->
                                        if (event.userId == ownId) {
                                            if (ready.compareAndSet(false, true)) emit(ChatRealtimeEvent.Ready)
                                        } else emit(ChatRealtimeEvent.Typing(event))
                                    }
                                }
                            }
                            "ERROR" -> channel.close(IOException("The chat connection was rejected."))
                        }
                    } catch (error: Exception) {
                        channel.close(error)
                        return
                    }
                }
                if (pending.length > 1_048_576) channel.close(IOException("Invalid chat frame"))
            }

            private fun emit(event: ChatRealtimeEvent) {
                if (trySend(event).isFailure) channel.close(IOException("The chat connection needs to resync."))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                channel.close(IOException("The chat connection closed."))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                response?.close()
                channel.close(t)
            }
        })

        val sendJob = launch { typing.collect { if (ready.get()) socket.sendTyping(chatId, it) } }
        val handshake = launch {
            delay(10_000)
            if (!ready.get()) channel.close(IOException("The chat connection timed out."))
        }
        awaitClose {
            sendJob.cancel()
            handshake.cancel()
            if (ready.get()) socket.sendTyping(chatId, false)
            if (!socket.close(1000, null)) socket.cancel()
        }
    }

    private fun WebSocket.sendTyping(chatId: Long, typing: Boolean) =
        send("SEND\ndestination:/app/chats/$chatId/typing\ncontent-type:application/json\n\n{\"typing\":$typing}$END")

    private companion object {
        const val END = "\u0000"
    }
}
