package feature.map.ui

import android.graphics.Color
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.evmap.mobileapp.core.data.dto.MapClusterDto
import com.evmap.mobileapp.feature.map.data.DeviceLocation
import com.evmap.mobileapp.feature.map.ui.MapCameraCommand
import core.ui.model.MapCameraUi
import core.ui.model.MapPinUi
import core.ui.model.MapViewport
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val EVENTS_SOURCE = "evmap-events"
private const val CLUSTERS_SOURCE = "evmap-clusters"
private const val LOCATION_SOURCE = "evmap-location"
private const val CLUSTER_LABELS = "evmap-cluster-labels"

internal enum class MapStyleStatus { LOADING, READY, ERROR }

@Composable
internal fun MapSurface(
    initialCamera: MapCameraUi,
    pins: List<MapPinUi>,
    clusters: List<MapClusterDto>,
    location: DeviceLocation?,
    commands: Flow<MapCameraCommand>,
    styleAttempt: Int,
    onStyleStatus: (MapStyleStatus) -> Unit,
    onViewport: (MapCameraUi, MapViewport) -> Unit,
    onPins: (Set<String>) -> Unit,
    onCluster: (Double, Double) -> Unit,
    onBackground: () -> Unit,
    modifier: Modifier = Modifier,
    styleUrl: String = OPEN_FREE_MAP_STYLE,
    pickerPoints: List<Pair<Double,Double>> = emptyList(),
    selectedGeometry: String? = null,
    onPick: ((Double,Double)->Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val view = remember(context, lifecycle) {
        MapLibre.getInstance(context)
        MapView(context, MapLibreMapOptions().textureMode(true).attributionEnabled(false))
    }
    var map by remember(view) { mutableStateOf<MapLibreMap?>(null) }
    var style by remember(view) { mutableStateOf<Style?>(null) }
    val viewportChanged by rememberUpdatedState(onViewport)
    val styleStatus by rememberUpdatedState(onStyleStatus)
    val pinClicked by rememberUpdatedState(onPins)
    val clusterClicked by rememberUpdatedState(onCluster)
    val backgroundClicked by rememberUpdatedState(onBackground)
    val pointPicked by rememberUpdatedState(onPick)

    DisposableEffect(view, lifecycle) {
        var active = true
        var started = false
        var resumed = false
        var instance: MapLibreMap? = null
        var idle: MapLibreMap.OnCameraIdleListener? = null
        var click: MapLibreMap.OnMapClickListener? = null
        val styleFailed = MapView.OnDidFailLoadingMapListener {
            if (active && style == null) styleStatus(MapStyleStatus.ERROR)
        }
        view.onCreate(null)
        view.addOnDidFailLoadingMapListener(styleFailed)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> { view.onStart(); started = true }
                Lifecycle.Event.ON_RESUME -> { view.onResume(); resumed = true }
                Lifecycle.Event.ON_PAUSE -> { view.onPause(); resumed = false }
                Lifecycle.Event.ON_STOP -> { view.onStop(); started = false }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        view.getMapAsync { ready ->
            if (!active) return@getMapAsync
            instance = ready
            ready.setMaxZoomPreference(22.0)
            ready.cameraPosition = CameraPosition.Builder()
                .target(LatLng(initialCamera.lat, initialCamera.lng)).zoom(initialCamera.zoom.toDouble()).build()
            idle = MapLibreMap.OnCameraIdleListener {
                if (active && view.width > 0 && view.height > 0) ready.reportViewport(viewportChanged)
            }.also(ready::addOnCameraIdleListener)
            click = MapLibreMap.OnMapClickListener { point ->
                if(pointPicked!=null) { pointPicked?.invoke(point.latitude,point.longitude); return@OnMapClickListener true }
                val pixel = ready.projection.toScreenLocation(point)
                val margin = 8 * view.resources.displayMetrics.density
                val hit = RectF(pixel.x - margin, pixel.y - margin, pixel.x + margin, pixel.y + margin)
                val events = ready.queryRenderedFeatures(hit, EVENTS_SOURCE, "evmap-area-fill", "evmap-area-line")
                val ids = events.mapNotNullTo(linkedSetOf()) { it.getStringProperty("eventId") }
                val cluster = ready.queryRenderedFeatures(hit, CLUSTERS_SOURCE).firstOrNull()?.geometry() as? Point
                when {
                    ids.isNotEmpty() -> pinClicked(ids)
                    cluster != null -> clusterClicked(cluster.latitude(), cluster.longitude())
                    else -> backgroundClicked()
                }
                true
            }.also(ready::addOnMapClickListener)
            map = ready
            view.post { if (active && view.width > 0 && view.height > 0) ready.reportViewport(viewportChanged) }
        }
        onDispose {
            active = false
            lifecycle.removeObserver(observer)
            idle?.let { instance?.removeOnCameraIdleListener(it) }
            click?.let { instance?.removeOnMapClickListener(it) }
            view.removeOnDidFailLoadingMapListener(styleFailed)
            if (resumed) view.onPause()
            if (started) view.onStop()
            view.onDestroy()
        }
    }

    val readyMap = map
    DisposableEffect(readyMap, styleAttempt, styleUrl) {
        var active = true
        if (readyMap != null) {
            style = null
            styleStatus(MapStyleStatus.LOADING)
            readyMap.setStyle(Style.Builder().fromUri(styleUrl)) { loaded ->
                if (active) {
                    loaded.addMapLayers()
                    loaded.addSource(GeoJsonSource("evmap-shapes", FeatureCollection.fromFeatures(emptyList<Feature>())))
                    loaded.addLayer(org.maplibre.android.style.layers.FillLayer("evmap-area-fill","evmap-shapes").withProperties(fillColor("#5267DF"),fillOpacity(0.2f)))
                    loaded.addLayer(org.maplibre.android.style.layers.LineLayer("evmap-area-line","evmap-shapes").withProperties(lineColor("#5267DF"),lineWidth(3f)))
                    loaded.addLayer(CircleLayer("evmap-area-points","evmap-shapes").withProperties(circleRadius(5f),circleColor("#5267DF")))
                    style = loaded
                    styleStatus(MapStyleStatus.READY)
                    if (view.width > 0 && view.height > 0) readyMap.reportViewport(viewportChanged)
                }
            }
        }
        onDispose { active = false }
    }
    LaunchedEffect(readyMap, commands) {
        if (readyMap != null) commands.collect { readyMap.applyCommand(it, view.resources.displayMetrics.density) }
    }
    LaunchedEffect(readyMap, styleAttempt, styleUrl) {
        if (readyMap != null) {
            delay(20_000)
            if (style == null) styleStatus(MapStyleStatus.ERROR)
        }
    }
    LaunchedEffect(style, pins, clusters, location) {
        style?.render(pins, clusters, location)
    }
    LaunchedEffect(style,pickerPoints,selectedGeometry,pins) {
        val shapes=mutableListOf<Feature>()
        pins.forEach { pin->pin.geometry?.let { raw->runCatching { Feature.fromJson("{\"type\":\"Feature\",\"properties\":{},\"geometry\":$raw}").apply {addStringProperty("eventId",pin.id)} }.getOrNull()?.let(shapes::add) } }
        selectedGeometry?.let { raw->runCatching { Feature.fromJson("{\"type\":\"Feature\",\"properties\":{},\"geometry\":$raw}") }.getOrNull()?.let(shapes::add) }
        pickerPoints.forEach { (lat,lng)->shapes.add(Feature.fromGeometry(Point.fromLngLat(lng,lat))) }
        if(pickerPoints.size>=2) {
            val coordinates=pickerPoints.map { org.json.JSONArray(listOf(it.second,it.first)) }.toMutableList()
            val geometry=if(pickerPoints.size>=3) { coordinates.add(coordinates.first());org.json.JSONObject().put("type","Polygon").put("coordinates",org.json.JSONArray().put(org.json.JSONArray(coordinates))) } else org.json.JSONObject().put("type","LineString").put("coordinates",org.json.JSONArray(coordinates))
            shapes.add(Feature.fromJson(org.json.JSONObject().put("type","Feature").put("properties",org.json.JSONObject()).put("geometry",geometry).toString()))
        }
        style?.getSourceAs<GeoJsonSource>("evmap-shapes")?.setGeoJson(FeatureCollection.fromFeatures(shapes))
    }
    LaunchedEffect(map,selectedGeometry) {
        if(map!=null && selectedGeometry!=null) runCatching {
            val points=mutableListOf<LatLng>()
            fun collect(value: org.json.JSONArray) { if(value.length()>0 && value.opt(0) is Number) { points.add(LatLng(value.getDouble(1),value.getDouble(0))) } else for(i in 0 until value.length()) collect(value.getJSONArray(i)) }
            collect(org.json.JSONObject(selectedGeometry).getJSONArray("coordinates"))
            if(points.size==1) map?.animateCamera(CameraUpdateFactory.newLatLngZoom(points.first(),14.0))
            else if(points.isNotEmpty()) map?.animateCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.Builder().includes(points).build(),60))
        }
    }
    AndroidView(factory = { view }, modifier = modifier)
}

private fun MapLibreMap.reportViewport(onViewport: (MapCameraUi, MapViewport) -> Unit) {
    val position = cameraPosition
    val center = position.target ?: return
    val bounds = projection.visibleRegion.latLngBounds
    onViewport(
        MapCameraUi(center.latitude, center.longitude, position.zoom.toFloat()),
        MapViewport(
            bounds.latitudeSouth, bounds.longitudeWest, bounds.latitudeNorth, bounds.longitudeEast,
            position.zoom.toInt().coerceIn(0, 22),
        ),
    )
}

private fun MapLibreMap.applyCommand(command: MapCameraCommand, density: Float) {
    val update = when (command) {
        is MapCameraCommand.Center -> with(command.camera) {
            CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), zoom.toDouble())
        }
        is MapCameraCommand.Zoom -> CameraUpdateFactory.zoomBy(command.delta)
        is MapCameraCommand.Fit -> {
            val points = command.pins.map { LatLng(it.lat, it.lng) }.distinct()
            if (points.size == 1) CameraUpdateFactory.newLatLngZoom(points.single(), 14.0)
            else {
                val bounds = LatLngBounds.Builder().includes(points).build()
                val padding = (64 * density).toInt()
                val fit = getCameraForLatLngBounds(bounds, intArrayOf(padding, padding, padding, padding))
                val target = fit?.target ?: bounds.center
                CameraUpdateFactory.newLatLngZoom(target, fit?.zoom?.coerceAtMost(15.0) ?: 12.0)
            }
        }
    }
    animateCamera(update)
}

private fun Style.addMapLayers() {
    for (id in listOf(EVENTS_SOURCE, CLUSTERS_SOURCE, LOCATION_SOURCE)) {
        addSource(GeoJsonSource(id, FeatureCollection.fromFeatures(emptyList<Feature>())))
    }
    addLayer(CircleLayer(LOCATION_SOURCE, LOCATION_SOURCE).withProperties(
        circleRadius(8f), circleColor(Color.rgb(22, 113, 240)), circleStrokeColor(Color.WHITE), circleStrokeWidth(2f),
    ))
    addLayer(CircleLayer(EVENTS_SOURCE, EVENTS_SOURCE).withProperties(
        circleRadius(8f), circleColor(Color.rgb(108, 61, 170)), circleStrokeColor(Color.WHITE), circleStrokeWidth(2f),
    ))
    addLayer(CircleLayer(CLUSTERS_SOURCE, CLUSTERS_SOURCE).withProperties(
        circleRadius(18f), circleColor(Color.rgb(84, 48, 142)), circleStrokeColor(Color.WHITE), circleStrokeWidth(2f),
    ))
    addLayer(SymbolLayer(CLUSTER_LABELS, CLUSTERS_SOURCE).withProperties(
        textField(Expression.get("count")), textFont(arrayOf("Noto Sans Bold")),
        textColor(Color.WHITE), textSize(13f), textAllowOverlap(true),
    ))
}

private fun Style.render(pins: List<MapPinUi>, clusters: List<MapClusterDto>, location: DeviceLocation?) {
    getSourceAs<GeoJsonSource>(EVENTS_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(pins.map {
        Feature.fromGeometry(Point.fromLngLat(it.lng, it.lat)).apply { addStringProperty("eventId", it.id) }
    }))
    getSourceAs<GeoJsonSource>(CLUSTERS_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(clusters.map {
        Feature.fromGeometry(Point.fromLngLat(it.point.lng, it.point.lat)).apply { addStringProperty("count", it.count.toString()) }
    }))
    val position = location?.let { Feature.fromGeometry(Point.fromLngLat(it.lng, it.lat)) }
    getSourceAs<GeoJsonSource>(LOCATION_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(listOfNotNull(position)))
}
