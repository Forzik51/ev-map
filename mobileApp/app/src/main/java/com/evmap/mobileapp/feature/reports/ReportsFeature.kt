package feature.reports

import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.evmap.mobileapp.feature.reports.ui.AdminModerationScreen
import com.evmap.mobileapp.feature.reports.ui.AdminModerationViewModel
import com.evmap.mobileapp.feature.reports.ui.ReportCreateScreen
import com.evmap.mobileapp.feature.reports.ui.ReportCreateViewModel
import com.evmap.mobileapp.feature.reports.ui.ReportModerationScreen
import com.evmap.mobileapp.feature.reports.ui.ReportModerationViewModel
import core.navigation.FeatureEntry

object ReportsFeature : FeatureEntry {
    override val route = "reports"
    const val ModerationRoute = "admin/moderation"

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
            val state by viewModel.state.collectAsStateWithLifecycle()
            val context = LocalContext.current

            LaunchedEffect(state.submitted) {
                if (state.submitted) {
                    viewModel.consumeSubmitted()
                    Toast.makeText(context, "Report submitted", Toast.LENGTH_SHORT).show()
                    onNavigateUp()
                }
            }

            ReportCreateScreen(
                state = state,
                onBack = onNavigateUp,
                onSelectCategory = viewModel::selectCategory,
                onDescriptionChange = viewModel::updateDescription,
                onRetryCategories = viewModel::loadCategories,
                onSubmit = { viewModel.submit(eventId) }
            )
        }

        navGraphBuilder.composable(route = ModerationRoute) {
            val viewModel: AdminModerationViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

            AdminModerationScreen(
                state = state,
                onBack = onNavigateUp,
                onRefresh = viewModel::refresh,
                onFilter = viewModel::selectFilter,
                onOpenReport = { onNavigateToRoute("$ModerationRoute/$it") },
                onSubjectChange = viewModel::updateSubject,
                onBodyChange = viewModel::updateBody,
                onUserIdChange = viewModel::updateUserId,
                onSendToAll = viewModel::sendToAll,
                onSendToUser = viewModel::sendToUser,
                onCategoryDecision = viewModel::decideCategory
            )
        }

        navGraphBuilder.composable(
            route = "$ModerationRoute/{reportId}",
            arguments = listOf(navArgument("reportId") { type = NavType.LongType }),
        ) {
            val viewModel: ReportModerationViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
            LaunchedEffect(state.deleted) { if (state.deleted) onNavigateUp() }
            ReportModerationScreen(
                state = state,
                onBack = onNavigateUp,
                onRefresh = viewModel::refresh,
                onOpenEvent = { onNavigateToRoute("events/$it") },
                onChooseOutcome = viewModel::chooseOutcome,
                onEditReason = viewModel::editReason,
                onCancelDecision = viewModel::cancelDecision,
                onSubmitDecision = viewModel::submitDecision,
                onDelete = viewModel::delete,
            )
        }
    }
}
