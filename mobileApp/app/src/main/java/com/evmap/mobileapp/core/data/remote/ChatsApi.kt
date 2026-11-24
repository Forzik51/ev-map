package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.ChatDto
import com.evmap.mobileapp.core.data.dto.ChatReadDto
import com.evmap.mobileapp.core.data.dto.CreateChatRequest
import com.evmap.mobileapp.core.data.dto.MarkChatReadRequest
import com.evmap.mobileapp.core.data.dto.MessageDto
import com.evmap.mobileapp.core.data.dto.SendMessageRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface ChatsApi {
    @POST("api/chats")
    suspend fun create(@Body request: CreateChatRequest): Long

    @POST("api/chats/direct/{peerId}")
    suspend fun openDirect(@Path("peerId") peerId: Long): Long

    @GET("api/chats")
    suspend fun mine(): List<ChatDto>

    @GET("api/chats/{chatId}")
    suspend fun info(@Path("chatId") chatId: Long): ChatDto

    @GET("api/chats/{chatId}/messages")
    suspend fun messages(
        @Path("chatId") chatId: Long,
        @Query("beforeId") beforeId: Long? = null,
        @Query("afterId") afterId: Long? = null,
        @Query("limit") limit: Int? = null,
    ): List<MessageDto>

    @POST("api/chats/{chatId}/messages")
    suspend fun send(
        @Path("chatId") chatId: Long,
        @Body request: SendMessageRequest,
    ): MessageDto

    @Multipart
    @POST("api/chats/{chatId}/attachments")
    suspend fun upload(
        @Path("chatId") chatId: Long,
        @Query("kind") kind: String,
        @Query("clientMessageId") clientMessageId: String,
        @Query("durationMs") durationMs: Long?,
        @Part file: MultipartBody.Part,
    ): MessageDto

    @Streaming
    @GET("api/chats/{chatId}/attachments/{attachmentId}")
    suspend fun download(@Path("chatId") chatId: Long, @Path("attachmentId") attachmentId: Long): ResponseBody

    @GET("api/chats/{chatId}/read")
    suspend fun readStates(@Path("chatId") chatId: Long): List<ChatReadDto>

    @POST("api/chats/{chatId}/read")
    suspend fun markRead(
        @Path("chatId") chatId: Long,
        @Body request: MarkChatReadRequest,
    ): Response<Unit>

    @GET("api/chats/search")
    suspend fun search(@Query("query") query: String): List<ChatDto>

    @PATCH("api/chats/{chatId}/name")
    suspend fun rename(
        @Path("chatId") chatId: Long,
        @Query("name") name: String,
    ): Response<Unit>

    @DELETE("api/chats/{chatId}")
    suspend fun delete(@Path("chatId") chatId: Long): Response<Unit>
}
