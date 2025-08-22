package feature.feed

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import core.navigation.FeatureEntry

object FeedFeature : FeatureEntry {
    override val route = "feed"
    
    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.composable(route) {
            // val viewModel: FeedViewModel = hiltViewModel()
            // val state by viewModel.state.collectAsState()
            // 
            // FeedScreen(
            //     query = state.query,
            //     items = state.items,
            //     currentRoute = route,
            //     onQueryChange = viewModel::updateQuery,
            //     onOpenItem = { eventId ->
            //         onNavigateToRoute("events/$eventId")
            //     },
            //     onOpenSearch = {
            //         // TODO: Navigate to search screen or expand search
            //     },
            //     onOpenCreate = {
            //         onNavigateToRoute("events/create")
            //     },
            //     onNavigate = onNavigateToRoute
            // )
        }
    }
}
