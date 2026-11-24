package com.evmap.mobileapp.feature.saved.data

import com.evmap.mobileapp.core.data.dto.CreateListRequest
import com.evmap.mobileapp.core.data.remote.ListsApi
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListsRepository @Inject constructor(private val api: ListsApi) {
    suspend fun mine() = call { api.mine() }
    suspend fun details(id: Long) = call { api.details(id) }
    suspend fun create(name: String) = call { api.create(CreateListRequest(name)) }
    suspend fun rename(id: Long, name: String) = call { api.rename(id, name).requireSuccess() }
    suspend fun delete(id: Long) = call { api.delete(id).requireSuccess() }
    suspend fun addEvent(id: Long, eventId: Long) = call { api.addEvent(id, eventId).requireSuccess() }
    suspend fun removeEvent(id: Long, eventId: Long) = call { api.removeEvent(id, eventId).requireSuccess() }

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
}
