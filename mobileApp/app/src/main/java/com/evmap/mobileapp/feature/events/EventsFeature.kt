package feature.events

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import core.navigation.FeatureEntry
import core.ui.model.EventDraftUi
import feature.events.ui.EventCreateScreen
import feature.events.ui.EventDetailsScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EventsFeature : FeatureEntry {
    override val route = "events"
    
    const val eventsListRoute = "events/list"
    const val eventDetailsRoute = "events/{eventId}"
    const val createEventRoute = "events/create"
    
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.navigation(
            startDestination = eventsListRoute,
            route = route
        ) {
            composable(eventsListRoute) {
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
            
            composable(eventDetailsRoute) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId")
                // val viewModel: EventDetailsViewModel = hiltViewModel()
                // val state by viewModel.state.collectAsState()

                // EventDetailsScreen(
                //     ui = state.eventDetails,
                //     currentRoute = "events",
                //     onBack = onNavigateUp,
                //     onShare = {
                //         // TODO: Implement sharing
                //     },
                //     onOpenMap = {
                //         onNavigateToRoute("map")
                //     },
                //     onOpenOrganizer = {
                //         // TODO: Navigate to organizer profile
                //     },
                //     onOpenCategory = { category ->
                //         // TODO: Navigate to category search/filter
                //     },
                //     onNavigate = onNavigateToRoute
                // )
            }
            
            composable(createEventRoute) {
                // TODO: Implement with ViewModel
                // val viewModel: EventCreateViewModel = hiltViewModel()
                // val state by viewModel.state.collectAsState()
                
                // Local state for demonstration - this would be in ViewModel
                var currentDraft by remember {
                    mutableStateOf(
                        EventDraftUi(
                            title = "",
                            subtitle = "",
                            description = "",
                            date = "08/17/2025",
                            location = "Warsaw",
                            categories = emptySet(),
                            availableCategories = listOf(
                                "Music", "Art", "Food", "Technology", "Sports", 
                                "Business", "Health", "Education", "Travel", "Gaming",
                                "Conference", "Workshop", "Networking", "Entertainment",
                                "Festival", "Competition", "Exhibition", "Social"
                            ),
                            media = listOf(
                                "https://example.com/image1.jpg",
                                "https://example.com/image2.jpg"
                            ),
                            categoriesQuery = ""
                        )
                    )
                }
                
                var showDatePicker by remember { mutableStateOf(false) }

                EventCreateScreen(
                    draft = currentDraft,
                    showDatePicker = showDatePicker,
                    onTitleChange = { title ->
                        currentDraft = currentDraft.copy(title = title)
                        // TODO: viewModel::updateTitle
                    },
                    onSubtitleChange = { subtitle ->
                        currentDraft = currentDraft.copy(subtitle = subtitle)
                        // TODO: viewModel::updateSubtitle
                    },
                    onDescriptionChange = { description ->
                        currentDraft = currentDraft.copy(description = description)
                        // TODO: viewModel::updateDescription
                    },
                    onPickDate = {
                        showDatePicker = true
                        // TODO: viewModel::showDatePicker
                    },
                    onDateSelected = { dateMillis ->
                        dateMillis?.let { millis ->
                            val formatter = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                            val formattedDate = formatter.format(Date(millis))
                            currentDraft = currentDraft.copy(date = formattedDate)
                            // TODO: viewModel::updateDate(formattedDate)
                        }
                    },
                    onDatePickerDismiss = {
                        showDatePicker = false
                        // TODO: viewModel::hideDatePicker
                    },
                    onPickLocation = {
                        // TODO: Show location picker
                    },
                    onCategoriesQueryChange = { query ->
                        currentDraft = currentDraft.copy(categoriesQuery = query)
                        // TODO: viewModel::updateCategoriesQuery(query)
                    },
                    onToggleCategory = { category ->
                        val updatedCategories = if (category in currentDraft.categories) {
                            currentDraft.categories - category
                        } else {
                            currentDraft.categories + category
                        }
                        currentDraft = currentDraft.copy(categories = updatedCategories)
                        // TODO: viewModel::toggleCategory(category)
                    },
                    onAddPhoto = {
                        // TODO: Show photo picker
                    },
                    onRemovePhoto = { index ->
                        val updatedMedia = currentDraft.media.toMutableList().apply {
                            if (index < size) removeAt(index)
                        }
                        currentDraft = currentDraft.copy(media = updatedMedia)
                        // TODO: viewModel::removePhoto(index)
                    },
                    onSubmit = {
                        // TODO: viewModel.createEvent { eventId ->
                        //     onNavigateToRoute("events/$eventId")
                        // }
                        onNavigateUp()
                    },
                    onBack = onNavigateUp
                )
            }
        }
    }
}
