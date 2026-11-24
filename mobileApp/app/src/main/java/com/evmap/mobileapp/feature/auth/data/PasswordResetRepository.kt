package com.evmap.mobileapp.feature.auth.data

import com.evmap.mobileapp.core.data.dto.*
import com.evmap.mobileapp.core.data.remote.AuthApi
import com.evmap.mobileapp.core.data.session.TokenStore
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

interface PasswordResetGateway {
    suspend fun requestCode(email: String): Result<ForgotPasswordResponse>
    suspend fun verifyCode(requestId: String, code: String): Result<VerifyPasswordCodeResponse>
    suspend fun resetPassword(token: String, password: String): Result<Unit>
}

data class PasswordResetApiError(val code: String? = null, val message: String? = null, val retryAfterSeconds: Long? = null)
class PasswordResetFailure(val code: String?, val retryAfterSeconds: Long?, message: String) : RuntimeException(message)

@Singleton
class PasswordResetRepository @Inject constructor(
    // Recovery must work even when a previously stored access token is expired or revoked.
    @Named("refreshAuthApi") private val api: AuthApi,
    private val tokenStore: TokenStore,
    moshi: Moshi,
) : PasswordResetGateway {
    private val errorAdapter = moshi.adapter(PasswordResetApiError::class.java)

    override suspend fun requestCode(email: String) = call { api.forgotPassword(ForgotPasswordRequest(email)) }
    override suspend fun verifyCode(requestId: String, code: String) =
        call { api.verifyPasswordCode(VerifyPasswordCodeRequest(requestId, code)) }
    override suspend fun resetPassword(token: String, password: String) = call {
        api.resetPassword(ResetPasswordRequest(token, password))
        tokenStore.clear()
    }

    private suspend fun <T> call(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: HttpException) {
        val error = runCatching { ex.response()?.errorBody()?.string()?.let(errorAdapter::fromJson) }.getOrNull()
        val retry = error?.retryAfterSeconds ?: ex.response()?.headers()?.get("Retry-After")?.toLongOrNull()
        Result.failure(PasswordResetFailure(error?.code, retry, when (ex.code()) {
            429 -> "Too many requests. Please wait before trying again."
            400 -> error?.message ?: "Check the entered data."
            else -> "Could not complete password recovery. Please try again."
        }))
    } catch (ex: Exception) {
        Result.failure(PasswordResetFailure(null, null, "Connection failed. Check your network and try again."))
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PasswordResetModule {
    @Binds
    abstract fun gateway(repository: PasswordResetRepository): PasswordResetGateway
}
