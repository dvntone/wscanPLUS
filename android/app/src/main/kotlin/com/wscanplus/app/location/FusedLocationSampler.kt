package com.wscanplus.app.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.concurrent.Executor

class FusedLocationSampler(
    context: Context,
    private val executor: Executor,
    private val onSample: (LocationSample) -> Unit,
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val callback =
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val sample = location.toLocationSample()
                onSample(sample)
            }
        }

    @SuppressLint("MissingPermission")
    fun start() {
        val request =
            LocationRequest
                .Builder(10_000L)
                .apply {
                    setMinUpdateIntervalMillis(5_000L)
                    setWaitForAccurateLocation(false)
                    setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                }.build()
        fusedLocationClient
            .lastLocation
            .addOnSuccessListener(executor) { location ->
                location?.let { onSample(it.toLocationSample()) }
            }
        fusedLocationClient.requestLocationUpdates(request, executor, callback)
    }

    fun stop() {
        fusedLocationClient.removeLocationUpdates(callback).addOnFailureListener { error ->
            Log.w(TAG, "Failed to remove fused location updates", error)
        }
    }

    private fun Location.toLocationSample(): LocationSample =
        LocationSample(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = if (hasAccuracy()) accuracy else null,
            altitudeMeters = if (hasAltitude()) altitude else null,
            speedKph = if (hasSpeed()) speed * 3.6f else null,
            capturedAt = time,
            provider = provider,
            isMock = isLocationMock(),
        )

    @Suppress("DEPRECATION")
    private fun Location.isLocationMock(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isMock
        } else {
            isFromMockProvider
        }

    companion object {
        private const val TAG = "FusedLocationSampler"
    }
}
