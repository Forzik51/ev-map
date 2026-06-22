package feature.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import core.navigation.FeatureEntry
import com.evmap.mobileapp.feature.events.ui.EventCreateViewModel
import feature.events.ui.EventCreateScreen
import feature.events.ui.EventDetailsScreen
import java.time.Instant
import java.time.ZoneId

object EventsFeature : FeatureEntry {
    override val route = "events"
    
    const val List = "events/list"
    const val Details = "events/{eventId}"
    const val Create = "events/create"
    
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.navigation(
            startDestination = List,
            route = route
        ) {
            composable(List) {
                // TODO: Implement EventsListScreen
                // EventsListScreen(
                //     onNavigateToDetails = { eventId ->
                //         onNavigateToRoute("events/$eventId")
                //     },
                //     onNavigateToCreate = {
                //         onNavigateToRoute(createEventRoute)
                //     }
                // )
            }

            composable(
                route = Details,
                arguments = listOf(navArgument("eventId") { type = NavType.LongType })
            ) { backStackEntry ->
                val eventId = checkNotNull(backStackEntry.arguments?.getLong("eventId"))

                EventDetailsScreen(
                    eventId = eventId,
                    /*ui = EventDetailsUi(
                        event = EventUi(
                            id = eventId,
                            title = "Event $eventId",
                            location = "—",
                            startsAt = "—",
                            description = "—",
                            imageUrl = null,
                            rating = 5.0f,
                            reviewCount = 0
                        ),
                        gallery = emptyList(),
                        fullDescription = null,
                        location = null,
                        date = null,
                        categories = emptyList(),
                        viewCount = null,
                        likeCount = null,
                        organizerName = null,
                        organizerId = null
                    ),*/
                    currentRoute = EventsFeature.route,
                    onBack = onNavigateUp,
                    onShare = {},
                    onReport = { onNavigateToRoute("reports/create/$eventId") },
                    onOpenMap = {},
                    onOpenOrganizer = {},
                    onOpenCategory = {},
                    onNavigate = onNavigateToRoute
                )
            }
            
            composable(Create) {
                val viewModel: EventCreateViewModel = hiltViewModel()
                val state by viewModel.state.collectAsState()
                var showDatePicker by remember { mutableStateOf(false) }

                LaunchedEffect(state.createdEventId) {
                    state.createdEventId?.let { eventId ->
                        viewModel.consumeCreatedEvent()
                        onNavigateToRoute("events/$eventId")
                    }
                }

                EventCreateScreen(
                    draft = state.draft,
                    showDatePicker = showDatePicker,
                    showLocationPicker = state.isLocationPickerOpen,
                    locationQuery = state.locationQuery,
                    locationResults = state.locationResults,
                    isSubmitting = state.loading,
                    errorMessage = state.error,
                    onTitleChange = viewModel::updateTitle,
                    onSubtitleChange = viewModel::updateSubtitle,
                    onDescriptionChange = viewModel::updateDescription,
                    onPickDate = {
                        showDatePicker = true
                    },
                    onDateSelected = { dateMillis ->
                        dateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .toString()
                            viewModel.updateDate(date)
                        }
                    },
                    onDatePickerDismiss = {
                        showDatePicker = false
                    },
                    onPickLocation = {
                        viewModel.openLocationPicker()
                    },
                    onLocationQueryChange = viewModel::updateLocationQuery,
                    onSelectLocation = viewModel::selectLocation,
                    onLocationPickerDismiss = viewModel::closeLocationPicker,
                    onCategoriesQueryChange = viewModel::updateCategoriesQuery,
                    onToggleCategory = viewModel::toggleCategory,
                    onAddPhoto = {
                        // TODO: Show photo picker
                    },
                    onRemovePhoto = viewModel::removePhoto,
                    onSubmit = {
                        viewModel.createEvent()
                    },
                    onBack = onNavigateUp
                )
            }
        }
    }
}
