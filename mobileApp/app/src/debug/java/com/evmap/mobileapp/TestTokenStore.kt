package com.evmap.mobileapp

import android.content.Context
import com.evmap.mobileapp.core.data.session.TokenStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TestTokenStoreEntryPoint {
    fun tokens(): TokenStore
}

fun testTokenStore(context: Context): TokenStore =
    EntryPointAccessors.fromApplication(context, TestTokenStoreEntryPoint::class.java).tokens()
