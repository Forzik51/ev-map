package com.evmap.mobileapp.core.data.remote

import com.evmap.mobileapp.core.data.dto.ChatDto
import com.evmap.mobileapp.core.data.dto.CreateChatRequest
import com.evmap.mobileapp.core.data.dto.MarkChatReadRequest
import com.evmap.mobileapp.core.data.dto.MessageDto
import com.evmap.mobileapp.core.data.dto.SendMessageRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatsApi {
    @POST("api/chats")
    suspend fun create(@Body request: CreateChatRequest): Long

    @GET("api/chats")
    suspend fun mine(): List<ChatDto>

    @GET("api/chats/{chatId}")
    suspend fun info(@Path("chatId") chatId: Long): ChatDto

    @GET("api/chats/{chatId}/messages")
    suspend fun messages(
        @Path("chatId") chatId: Long,
        @Query("beforeId") beforeId: Long? = null,
        @Query("limit") limit: Int? = null,
    ): List<MessageDto>

    @POST("api/chats/{chatId}/messages")
    suspend fun send(
        @Path("chatId") chatId: Long,
        @Body request: SendMessageRequest,
    ): MessageDto

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
