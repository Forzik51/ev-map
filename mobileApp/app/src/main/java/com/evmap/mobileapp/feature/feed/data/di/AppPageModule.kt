package com.evmap.mobileapp.feature.feed.data.di


import com.evmap.mobileapp.core.data.remote.AuthApi
import com.evmap.mobileapp.core.data.remote.ChatsApi
import com.evmap.mobileapp.core.data.remote.EventsApi
import com.evmap.mobileapp.core.data.remote.ListsApi
import com.evmap.mobileapp.core.data.remote.MapApi
import com.evmap.mobileapp.core.data.remote.NotificationsApi
import com.evmap.mobileapp.core.data.remote.ReportsApi
import com.evmap.mobileapp.core.data.remote.SearchApi
import com.evmap.mobileapp.core.data.remote.UsersApi
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
    fun authApi(retrofit: Retrofit): AuthApi =
        retrofit.create(AuthApi::class.java)

    @Provides @Singleton
    fun eventsApi(retrofit: Retrofit): EventsApi =
        retrofit.create(EventsApi::class.java)

    @Provides @Singleton
    fun usersApi(retrofit: Retrofit): UsersApi =
        retrofit.create(UsersApi::class.java)

    @Provides @Singleton
    fun mapApi(retrofit: Retrofit): MapApi =
        retrofit.create(MapApi::class.java)

    @Provides @Singleton
    fun searchApi(retrofit: Retrofit): SearchApi =
        retrofit.create(SearchApi::class.java)

    @Provides @Singleton
    fun listsApi(retrofit: Retrofit): ListsApi =
        retrofit.create(ListsApi::class.java)

    @Provides @Singleton
    fun chatsApi(retrofit: Retrofit): ChatsApi =
        retrofit.create(ChatsApi::class.java)

    @Provides @Singleton
    fun reportsApi(retrofit: Retrofit): ReportsApi =
        retrofit.create(ReportsApi::class.java)

    @Provides @Singleton
    fun notificationsApi(retrofit: Retrofit): NotificationsApi =
        retrofit.create(NotificationsApi::class.java)
}
