package com.evmap.mobileapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.evmap.mobileapp.core.data.session.TokenStore
import dagger.hilt.android.AndroidEntryPoint
import feature.auth.AuthFeature
import feature.feed.FeedFeature
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var tokenStore: TokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val session by tokenStore.sessions.collectAsStateWithLifecycle()
            key(session?.userId) {
                val startDestination = if (session == null) AuthFeature.route else FeedFeature.route
                app.NavGraph(navController = rememberNavController(), startDestination = startDestination)
            }
        }
    }
}
