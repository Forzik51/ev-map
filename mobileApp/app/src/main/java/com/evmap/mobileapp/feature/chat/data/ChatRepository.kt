package com.evmap.mobileapp.feature.chat.data

import com.evmap.mobileapp.core.data.dto.MarkChatReadRequest
import com.evmap.mobileapp.core.data.dto.SendMessageRequest
import com.evmap.mobileapp.core.data.remote.ChatsApi
import com.evmap.mobileapp.core.data.session.TokenStore
import kotlinx.coroutines.CancellationException
import okhttp3.MultipartBody
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(private val api: ChatsApi, private val tokenStore: TokenStore) {
    fun currentUserId(): Long? = tokenStore.session()?.userId?.takeIf { it > 0 }

    suspend fun chats() = call { api.mine() }
    suspend fun openDirect(peerId: Long) = call { api.openDirect(peerId) }
    suspend fun chat(chatId: Long) = call { api.info(chatId) }
    suspend fun messages(chatId: Long, beforeId: Long? = null, afterId: Long? = null) =
        call { api.messages(chatId, beforeId, afterId, PAGE_SIZE) }

    suspend fun send(chatId: Long, text: String, clientId: String) = call { api.send(chatId, SendMessageRequest(text, clientId)) }
    suspend fun upload(chatId: Long, media: ChatMediaDraft, file: MultipartBody.Part) =
        call { api.upload(chatId, media.kind.name, media.clientId, media.durationMs, file) }
    suspend fun download(chatId: Long, attachmentId: Long) = call { api.download(chatId, attachmentId) }
    suspend fun readStates(chatId: Long) = call { api.readStates(chatId) }
    suspend fun markRead(chatId: Long, messageId: Long) = call { api.markRead(chatId, MarkChatReadRequest(messageId)).requireSuccess() }
    suspend fun delete(chatId: Long) = call { api.delete(chatId).requireSuccess() }

    private fun Response<Unit>.requireSuccess() {
        if (!isSuccessful) throw HttpException(this)
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    companion object {
        const val PAGE_SIZE = 50
    }
}
