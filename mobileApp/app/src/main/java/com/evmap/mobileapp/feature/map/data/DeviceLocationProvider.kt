package com.evmap.mobileapp.feature.map.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class DeviceLocation(val lat: Double, val lng: Double, val precise: Boolean)

internal class DeviceLocationProvider(context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val executor = ContextCompat.getMainExecutor(context)

    fun isEnabled(): Boolean = manager?.let(LocationManagerCompat::isLocationEnabled) == true

    @SuppressLint("MissingPermission") // The map checks runtime permission before starting a fix.
    suspend fun current(precise: Boolean): DeviceLocation? {
        val manager = manager ?: return null
        val providers = buildList {
            if (precise) add(LocationManager.GPS_PROVIDER)
            if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }
        for (provider in providers) {
            if (provider !in manager.allProviders || !manager.isProviderEnabled(provider)) continue
            val location = withTimeoutOrNull(if (provider == LocationManager.GPS_PROVIDER) 8_000L else 4_000L) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    val signal = CancellationSignal()
                    continuation.invokeOnCancellation { signal.cancel() }
                    LocationManagerCompat.getCurrentLocation(manager, provider, signal, executor) {
                        continuation.resume(it)
                    }
                }
            }
            if (location != null) return DeviceLocation(location.latitude, location.longitude, precise)
        }
        return null
    }
}
