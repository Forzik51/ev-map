package feature.reports

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.evmap.mobileapp.feature.reports.ui.AdminModerationScreen
import com.evmap.mobileapp.feature.reports.ui.AdminModerationViewModel
import com.evmap.mobileapp.feature.reports.ui.ReportCreateScreen
import com.evmap.mobileapp.feature.reports.ui.ReportCreateViewModel
import core.navigation.FeatureEntry

object ReportsFeature : FeatureEntry {
    override val route = "reports"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit
    ) {
        navGraphBuilder.composable(
            route = "reports/create/{eventId}",
            arguments = listOf(navArgument("eventId") { type = NavType.LongType })
        ) { backStackEntry ->
            val eventId = checkNotNull(backStackEntry.arguments?.getLong("eventId"))
            val viewModel: ReportCreateViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            LaunchedEffect(state.submitted) {
                if (state.submitted) {
                    viewModel.consumeSubmitted()
                    onNavigateUp()
                }
            }

            ReportCreateScreen(
                state = state,
                onBack = onNavigateUp,
                onSelectCategory = viewModel::selectCategory,
                onDescriptionChange = viewModel::updateDescription,
                onSubmit = { viewModel.submit(eventId) }
            )
        }

        navGraphBuilder.composable(route = "admin/moderation") {
            val viewModel: AdminModerationViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()

            AdminModerationScreen(
                state = state,
                onBack = onNavigateUp,
                onRefresh = viewModel::refresh,
                onUpdateStatus = viewModel::updateStatus,
                onDelete = viewModel::delete,
                onSubjectChange = viewModel::updateSubject,
                onBodyChange = viewModel::updateBody,
                onUserIdChange = viewModel::updateUserId,
                onSendToAll = viewModel::sendToAll,
                onSendToUser = viewModel::sendToUser
            )
        }
    }
}
