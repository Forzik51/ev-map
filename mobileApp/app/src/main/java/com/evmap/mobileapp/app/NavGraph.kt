package app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import feature.auth.AuthFeature
import feature.map.MapFeature

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = AuthFeature.route
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
    }
}
