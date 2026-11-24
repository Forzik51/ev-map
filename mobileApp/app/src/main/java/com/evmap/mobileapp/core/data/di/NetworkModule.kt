package com.evmap.mobileapp.core.data.di

import com.evmap.mobileapp.BuildConfig
import com.evmap.mobileapp.core.data.remote.AuthApi
import com.evmap.mobileapp.core.data.remote.AuthHeaderInterceptor
import com.evmap.mobileapp.core.data.remote.TokenRefreshAuthenticator
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    @Named("noAuthOkHttp")
    fun noAuthOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .apply {
            val logger = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            }
            addInterceptor(logger)
        }.build()

    @Provides @Singleton
    fun okHttp(
        authHeaderInterceptor: AuthHeaderInterceptor,
        tokenRefreshAuthenticator: TokenRefreshAuthenticator,
    ): OkHttpClient = OkHttpClient.Builder()
        .apply {
            val logger = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            }
            addInterceptor(authHeaderInterceptor)
            addInterceptor(logger)
            authenticator(tokenRefreshAuthenticator)
        }.build()

    @Provides @Singleton
    @Named("mediaOkHttp")
    fun mediaOkHttp(
        @Named("noAuthOkHttp") client: OkHttpClient,
        @Named("baseUrl") baseUrl: String,
        authHeader: AuthHeaderInterceptor,
        refresh: TokenRefreshAuthenticator,
    ): OkHttpClient {
        val backend = baseUrl.toHttpUrl()
        val chatAttachmentPath = Regex("^/api/chats/[0-9]+/attachments/[0-9]+$")
        fun isBackendMedia(url: okhttp3.HttpUrl): Boolean =
            url.scheme == backend.scheme && url.host == backend.host && url.port == backend.port &&
                (url.encodedPath.startsWith("/media/") || chatAttachmentPath.matches(url.encodedPath))

        return client.newBuilder()
            .addInterceptor { chain ->
                if (isBackendMedia(chain.request().url)) authHeader.intercept(chain)
                else chain.proceed(chain.request())
            }
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                val safeRequest = if (isBackendMedia(request.url)) request
                else request.newBuilder().removeHeader("Authorization").build()
                chain.proceed(safeRequest)
            }
            .authenticator { route, response ->
                if (isBackendMedia(response.request.url)) refresh.authenticate(route, response) else null
            }
            .build()
    }

    @Provides @Singleton
    fun moshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides @Singleton
    fun retrofit(
        ok: OkHttpClient,
        moshi: Moshi,
        @Named("baseUrl") baseUrl: String
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(ok)
        .build()

    @Provides @Singleton
    @Named("refreshRetrofit")
    fun refreshRetrofit(
        moshi: Moshi,
        @Named("baseUrl") baseUrl: String,
        @Named("noAuthOkHttp") ok: OkHttpClient
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(ok)
        .build()

    @Provides @Singleton
    @Named("refreshAuthApi")
    fun refreshAuthApi(
        @Named("refreshRetrofit") retrofit: Retrofit
    ): AuthApi = retrofit.create(AuthApi::class.java)
}
