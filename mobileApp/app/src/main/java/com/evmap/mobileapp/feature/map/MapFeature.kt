package feature.map

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.evmap.mobileapp.feature.map.data.DeviceLocationProvider
import com.evmap.mobileapp.feature.map.ui.MapViewModel
import core.navigation.FeatureEntry
import feature.map.ui.SearchMapScreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object MapFeature : FeatureEntry {
    override val route = "map"

    override fun registerGraph(
        navGraphBuilder: NavGraphBuilder,
        onNavigateToRoute: (String) -> Unit,
        onNavigateUp: () -> Unit,
    ) {
        navGraphBuilder.composable(route) {
            val viewModel: MapViewModel = hiltViewModel()
            MapRoute(viewModel, onNavigateToRoute)
        }
    }
}

@Composable
private fun MapRoute(viewModel: MapViewModel, navigate: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val provider = remember(context) { DeviceLocationProvider(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var locationJob by remember { mutableStateOf<Job?>(null) }
    var locating by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<LocationNotice?>(null) }

    fun locate() {
        val precise = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!precise && !context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) return
        notice = null
        if (!provider.isEnabled()) {
            notice = LocationNotice("Turn on location to find your position.", Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            return
        }
        locationJob?.cancel()
        locationJob = scope.launch {
            locating = true
            try {
                val location = provider.current(precise)
                if (location != null) viewModel.showLocation(location)
                else notice = LocationNotice("Location is unavailable. Tap My Location to try again.")
            } catch (_: SecurityException) {
                viewModel.clearLocation()
                notice = LocationNotice("Location permission is required to find your position.", Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            } finally {
                locating = false
            }
        }
    }

    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (context.hasLocationPermission()) locate()
        else {
            val canAsk = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_COARSE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.ACCESS_FINE_LOCATION)
            } == true
            notice = LocationNotice(
                "Location permission is required to find your position.",
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS.takeUnless { canAsk },
            )
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (!context.hasLocationPermission() ||
            state.location?.precise == true && !context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        ) viewModel.clearLocation()
        if (notice?.settings == Settings.ACTION_APPLICATION_DETAILS_SETTINGS && context.hasLocationPermission()) notice = null
        if (notice?.settings == Settings.ACTION_LOCATION_SOURCE_SETTINGS && provider.isEnabled()) notice = null
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { locationJob?.cancel() }

    val initialCamera = remember(viewModel) { viewModel.cameraPosition() }
    SearchMapScreen(
        state = state,
        initialCamera = initialCamera,
        cameraCommands = viewModel.commands,
        onViewport = viewModel::cameraStopped,
        onQueryChange = viewModel::updateQuery,
        onApplyFilters = viewModel::applyFilters,
        onPinClick = viewModel::selectPins,
        onPinChoice = viewModel::selectPin,
        onClusterClick = viewModel::selectCluster,
        onRecenter = viewModel::recenter,
        onZoomIn = { viewModel.zoom(1.0) },
        onZoomOut = { viewModel.zoom(-1.0) },
        onMyLocation = {
            if (context.hasLocationPermission()) locate()
            else if (notice?.settings == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                context.openLocationSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            } else {
                permissionRequest.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        },
        locating = locating,
        locationNotice = notice?.text,
        onLocationSettings = notice?.settings?.let { action -> { context.openLocationSettings(action) } },
        onDismissLocationNotice = { notice = null },
        onRetry = viewModel::retry,
        onOpenEvent = { navigate("events/$it") },
        onDismissPreview = viewModel::dismissPreview,
        onNavigate = navigate,
    )
}

private data class LocationNotice(val text: String, val settings: String? = null)

private fun Context.hasPermission(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.hasLocationPermission() =
    hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION) || hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

private fun Context.openLocationSettings(action: String) {
    val intent = Intent(action)
    if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
        intent.data = Uri.fromParts("package", packageName, null)
    }
    startActivity(intent)
}
