package com.evmap.mobileapp.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
            val navController = rememberNavController()
            val startDestination = if (tokenStore.isLoggedIn()) FeedFeature.route else AuthFeature.route
            app.NavGraph(navController = navController, startDestination = startDestination)
        }
    }
}
