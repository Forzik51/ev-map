package app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import feature.auth.AuthFeature
import feature.events.EventsFeature
import feature.feed.FeedFeature
import feature.follow.FollowFeature
import feature.map.MapFeature
import feature.profile.ProfileFeature
import feature.saved.SavedFeature
import feature.settings.SettingsFeature

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = FeedFeature.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        AuthFeature.registerGraph(
            navGraphBuilder = this,
            onNavigateToRoute = { route ->
                navController.navigate(route)
            },
            onNavigateUp = {
                navController.navigateUp()
            }
        )

        MapFeature.registerGraph(
            navGraphBuilder = this,
            onNavigateToRoute = { route ->
                navController.navigate(route)
            },
            onNavigateUp = {
                navController.navigateUp()
            }
        )

        FeedFeature.registerGraph(
            navGraphBuilder = this,
            onNavigateToRoute = { route ->
                navController.navigate(route)
            },
            onNavigateUp = {
                navController.navigateUp()
            }
        )

        EventsFeature.registerGraph(
            navGraphBuilder = this,
            onNavigateToRoute = { route ->
                navController.navigate(route)
            },
            onNavigateUp = {
                navController.navigateUp()
            }
        )

        ProfileFeature.registerGraph(
            navGraphBuilder = this,
            onNavigateToRoute = { route ->
                navController.navigate(route)
            },
            onNavigateUp = {
                navController.navigateUp()
            }
        )

        FollowFeature.registerGraph(
            navGraphBuilder = this,
            onNavigateToRoute = { route ->
                navController.navigate(route)
            },
            onNavigateUp = {
                navController.navigateUp()
            }
        )

        SavedFeature.registerGraph(
            navGraphBuilder = this,
            onNavigateToRoute = { route ->
                navController.navigate(route)
            },
            onNavigateUp = {
                navController.navigateUp()
            }
        )

        SettingsFeature.registerGraph(
            navGraphBuilder = this,
            onNavigateToRoute = { route ->
                navController.navigate(route)
            },
            onNavigateUp = {
                navController.navigateUp()
            }
        )
    }
}
