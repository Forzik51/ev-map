package com.evmap.mobileapp.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.evmap.mobileapp.BuildConfig
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Named

@HiltAndroidApp
class App : Application(), ImageLoaderFactory {
    @Inject @Named("mediaOkHttp")
    lateinit var mediaClient: OkHttpClient

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this).okHttpClient(mediaClient).build()

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("App", "BASE_URL=${BuildConfig.BASE_URL}")
    }
}

