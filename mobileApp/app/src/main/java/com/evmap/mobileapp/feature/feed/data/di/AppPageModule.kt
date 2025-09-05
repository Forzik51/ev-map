package com.evmap.mobileapp.feature.feed.data.di


import com.evmap.mobileapp.core.data.remote.EventsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppPageModule {
    @Provides @Singleton
    fun eventsApi(retrofit: Retrofit): EventsApi =
        retrofit.create(EventsApi::class.java)
}