package com.evmap.mobileapp.feature.map

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.core.content.ContextCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.evmap.mobileapp.app.MainActivity
import com.evmap.mobileapp.core.data.dto.AuthResponse
import com.evmap.mobileapp.testTokenStore
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.maps.MapView
import java.io.Closeable
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class MapNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun pansTheMapAndOpensAnEventFromACluster() {
        val store = testTokenStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val previous = store.session()
        val proxy = listOf("http.proxyHost", "http.proxyPort").associateWith(System::getProperty)
        MapFixture().use { fixture ->
            try {
                System.setProperty("http.proxyHost", "127.0.0.1")
                System.setProperty("http.proxyPort", fixture.port.toString())
                store.save(AuthResponse("fixture-access", "fixture-refresh", expiresInSeconds = 3600, userId = 1, username = "test"))
                ActivityScenario.launch(MainActivity::class.java).use { activity ->
                    compose.onNodeWithContentDescription("Map").performClick()
                    compose.waitUntil(45_000) {
                        compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Map ready"))
                            .fetchSemanticsNodes().isNotEmpty()
                    }
                    compose.waitUntil(15_000) { fixture.clusters.isNotEmpty() }
                    val mapBounds = Rect()
                    activity.onActivity {
                        val view = checkNotNull(findMapView(it.window.decorView))
                        val offset = IntArray(2).also(view::getLocationOnScreen)
                        mapBounds.set(offset[0], offset[1], offset[0] + view.width, offset[1] + view.height)
                    }
                    val initial = checkNotNull(fixture.clusters.last().queryParameter("minLng")).toDouble()
                    val y = mapBounds.centerY()
                    shell("input swipe ${mapBounds.left + mapBounds.width() / 4} $y ${mapBounds.right - mapBounds.width() / 4} $y 450")
                    compose.waitUntil(15_000) { abs(fixture.clusters.last().queryParameter("minLng")!!.toDouble() - initial) > 0.001 }
                    compose.onNodeWithContentDescription("Recenter").performClick()
                    compose.waitUntil(15_000) { abs(fixture.clusters.last().queryParameter("minLng")!!.toDouble() - initial) < 0.001 }
                    tapMapUntil(mapBounds) { fixture.bounds.isNotEmpty() }
                    awaitText("Retry events")
                    compose.onNodeWithText("Retry events").performClick()
                    compose.waitUntil(15_000) { fixture.bounds.size >= 2 }
                    tapMapUntil(mapBounds) { fixture.detailRequests.isNotEmpty() }
                    awaitText("Reported on the map")
                    compose.onNodeWithText("Reported on the map").performClick()
                    awaitText("Event page loaded")
                    assertTrue(fixture.clusters.any { it.queryParameter("zoom") == "11" })

                    val mode = InstrumentationRegistry.getArguments().getString("mapLocation")
                    if (mode != null) {
                        compose.onNodeWithContentDescription("Back").performClick()
                        verifyLocation(fixture, precise = mode == "precise")
                    }
                }
            } finally {
                proxy.forEach { (name, value) -> if (value == null) System.clearProperty(name) else System.setProperty(name, value) }
                if (previous == null) store.clear() else store.save(AuthResponse(
                    previous.accessToken, previous.refreshToken, previous.tokenType,
                    (previous.expiresAtMillis - System.currentTimeMillis()) / 1000, previous.userId, previous.username,
                ))
            }
        }
    }

    private fun awaitText(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun findMapView(view: View): MapView? {
        if (view is MapView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findMapView(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun tapMapUntil(bounds: Rect, clicked: () -> Boolean) {
        repeat(6) {
            if (clicked()) return
            shell("input tap ${bounds.centerX()} ${bounds.centerY()}")
            if (runCatching { compose.waitUntil(1_500, clicked) }.isSuccess) return
        }
        assertTrue("Tapping the map did not load the next map data", clicked())
    }

    private fun verifyLocation(fixture: MapFixture, precise: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val existing = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (existing != PackageManager.PERMISSION_GRANTED) {
            compose.onNodeWithContentDescription("My Location").performClick()
            clickPermissionButton("permission_deny_button")
            awaitText("Location permission is required to find your position.")
        }
        val provider = if (precise) "gps" else if (Build.VERSION.SDK_INT >= 31) "fused" else "network"
        shell("appops set com.android.shell android:mock_location allow")
        shell("cmd location set-location-enabled true")
        shell("cmd location providers add-test-provider $provider")
        shell("cmd location providers set-test-provider-enabled $provider true")
        try {
            val start = fixture.bounds.size
            compose.onNodeWithContentDescription("My Location").performClick()
            if (existing != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= 31) {
                    val accuracy = if (precise) "fine" else "coarse"
                    clickPermissionButton("permission_location_accuracy_radio_$accuracy")
                }
                clickPermissionButton("permission_allow_foreground_only_button")
            }
            val expectedFine = if (precise) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
            compose.waitUntil(15_000) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == expectedFine
            }
            shell("cmd location providers set-test-provider-location $provider --location 52.4,21.2 --accuracy 3")
            compose.waitUntil(20_000) {
                fixture.bounds.drop(start).any { bounds ->
                    val west = bounds.queryParameter("minLng")!!.toDouble()
                    val east = bounds.queryParameter("maxLng")!!.toDouble()
                    abs((west + east) / 2.0 - 21.2) < 0.08
                }
            }
        } finally {
            shell("cmd location providers remove-test-provider $provider")
        }
    }

    private fun clickPermissionButton(id: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        compose.waitUntil(15_000) {
            findPermissionButton(automation.rootInActiveWindow, id)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
        }
    }

    private fun findPermissionButton(node: AccessibilityNodeInfo?, id: String): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.viewIdResourceName?.endsWith("/$id") == true) return node
        for (index in 0 until node.childCount) {
            findPermissionButton(node.getChild(index), id)?.let { return it }
        }
        return null
    }

    private fun shell(command: String) {
        val result = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(result).bufferedReader().use { it.readText() }
    }

    private class MapFixture : Closeable {
        private val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        val port = server.localPort
        val clusters = CopyOnWriteArrayList<HttpUrl>()
        val bounds = CopyOnWriteArrayList<HttpUrl>()
        val detailRequests = CopyOnWriteArrayList<String>()
        private val page = """{"content":[],"number":0,"size":20,"totalElements":0,"totalPages":0,"first":true,"last":true}"""
        private val mapEvent = """{"eventId":5,"eventName":"Reported on the map","locationName":"Warsaw","geometryType":"POINT","point":{"lat":52.2297,"lng":21.0122},"regionGeoJson":null,"distanceMeters":null,"ratingAvg":4.5,"ratingsCount":2,"categories":[]}"""
        private val details = """{"eventId":5,"eventName":"Reported on the map","description":"Event preview","startsAt":"2030-03-22T10:00:00Z","endsAt":null,"locationName":"Warsaw","geometryType":"POINT","point":{"lat":52.2297,"lng":21.0122},"regionGeoJson":null,"imagePath":null,"ratingAvg":4.5,"ratingsCount":2,"repostsCount":0,"commentsCount":0,"categories":[]}"""

        init {
            thread(isDaemon = true, name = "Map API fixture") {
                while (!server.isClosed) runCatching { server.accept().use(::serve) }
            }
        }

        private fun serve(connection: Socket) {
            connection.soTimeout = 5_000
            val input = connection.getInputStream().bufferedReader()
            val url = input.readLine().split(' ')[1].toHttpUrl()
            while (!input.readLine().isNullOrEmpty()) Unit
            val (status, body) = respond(url)
            val bytes = body.encodeToByteArray()
            connection.getOutputStream().apply {
                write("HTTP/1.1 $status Fixture\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".encodeToByteArray())
                write(bytes)
                flush()
            }
        }

        private fun respond(url: HttpUrl): Pair<Int, String> = when (url.encodedPath) {
            "/api/events", "/api/events/feed", "/api/events/5/comments" -> 200 to page
            "/api/events/5/photos" -> 200 to "[]"
            "/api/map/events/clusters" -> {
                clusters += url
                200 to """[{"count":4,"point":{"lat":52.2297,"lng":21.0122}}]"""
            }
            "/api/map/events/bounds" -> {
                bounds += url
                if (bounds.size == 1) 503 to "{}" else 200 to "[$mapEvent]"
            }
            "/api/map/events/5" -> {
                detailRequests += url.encodedPath
                200 to details
            }
            "/api/events/5" -> 200 to """{"id":5,"name":"Reported on the map","description":"Event page loaded","startsAt":"2030-03-22T10:00:00Z","userId":2}"""
            else -> 404 to "{}"
        }

        override fun close() = server.close()
    }
}
