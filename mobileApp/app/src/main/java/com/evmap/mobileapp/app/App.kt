package com.evmap.mobileapp.app

import android.app.Application
import com.evmap.mobileapp.BuildConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("App", "BASE_URL=${BuildConfig.BASE_URL}")
    }
}

