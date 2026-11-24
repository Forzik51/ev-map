package app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import feature.auth.AuthFeature
import feature.chat.ChatFeature
import feature.events.EventsFeature
import feature.explore.ExploreFeature
import feature.feed.FeedFeature
import feature.follow.FollowFeature
import feature.map.MapFeature
import feature.profile.ProfileFeature
import feature.reports.ReportsFeature
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
                navController.navigate(route) {
                    if (route == AuthFeature.route || route == FeedFeature.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
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

        ExploreFeature.registerGraph(
            navGraphBuilder = this,
            onNavigateToRoute = { route -> navController.navigate(route) },
            onNavigateUp = { navController.navigateUp() }
        )

        EventsFeature.registerGraph(
            navGraphBuilder = this,
            navController = navController,
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

        ChatFeature.registerGraph(
            navGraphBuilder = this,
            onNavigateToRoute = { route ->
                navController.navigate(route)
            },
            onNavigateUp = {
                navController.navigateUp()
            }
        )

        ReportsFeature.registerGraph(
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
