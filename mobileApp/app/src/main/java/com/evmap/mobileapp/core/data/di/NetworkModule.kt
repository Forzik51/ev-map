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
