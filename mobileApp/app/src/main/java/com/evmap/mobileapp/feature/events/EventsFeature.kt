package feature.events

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.evmap.mobileapp.feature.events.ui.EventDetailsScreen
import com.evmap.mobileapp.feature.events.ui.EventFormScreen
import com.evmap.mobileapp.feature.events.ui.EventListScreen
import feature.saved.SavedFeature

object EventsFeature {
    private const val Route = "events"
    const val List = "events/list"
    private const val Details = "events/{eventId}"
    const val Create = "events/create"
    private const val Edit = "events/{eventId}/edit"
    private const val RefreshDetails = "refresh_event_details"

    fun registerGraph(navGraphBuilder: NavGraphBuilder, navController: NavHostController) {
        val eventIdArgument = listOf(navArgument("eventId") { type = NavType.LongType })
        navGraphBuilder.navigation(startDestination = List, route = Route) {
            composable(List) {
                EventListScreen(
                    onBack = { navController.navigateUp() },
                    onCreate = { navController.navigate(Create) },
                    onOpen = { navController.navigate("events/$it") },
                    onSave = { navController.navigate(SavedFeature.pickerRoute(it)) },
                )
            }
            composable(Details, arguments = eventIdArgument) { entry ->
                val id = checkNotNull(entry.arguments?.getLong("eventId"))
                val refresh by entry.savedStateHandle.getStateFlow(RefreshDetails, false).collectAsState()
                EventDetailsScreen(
                    onBack = { navController.navigateUp() },
                    onEdit = { navController.navigate("events/$id/edit") },
                    onDeleted = { navController.popBackStack() },
                    onReport = { navController.navigate("reports/create/$id") },
                    onSave = { navController.navigate(SavedFeature.pickerRoute(id.toString())) },
                    onNavigate = { navController.navigate(it) },
                    refreshAfterEdit = refresh,
                    onRefreshConsumed = { entry.savedStateHandle[RefreshDetails] = false },
                )
            }
            composable(Create) {
                EventFormScreen(
                    onBack = { navController.navigateUp() },
                    onSaved = { id ->
                        navController.navigate("events/$id") {
                            popUpTo(Create) { inclusive = true }
                        }
                    },
                    onOpenCatalog = {
                        navController.navigate(List) {
                            popUpTo(Create) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Edit, arguments = eventIdArgument) {
                EventFormScreen(
                    onBack = { navController.navigateUp() },
                    onSaved = {
                        navController.previousBackStackEntry?.savedStateHandle?.set(RefreshDetails, true)
                        navController.popBackStack()
                    },
                    onOpenCatalog = { navController.navigate(List) },
                )
            }
        }
    }
}
