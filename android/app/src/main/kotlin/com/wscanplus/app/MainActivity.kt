package com.wscanplus.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var rootLayout: LinearLayout
    private lateinit var actionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootLayout =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(48, 48, 48, 48)
            }
        statusView =
            TextView(this).apply {
                text = "wscan+ needs Wi-Fi scan permissions to start the scanner."
                textSize = 16f
            }
        actionButton =
            Button(this).apply {
                visibility = View.GONE
            }
        rootLayout.addView(statusView)
        rootLayout.addView(actionButton)
        setContentView(rootLayout)

        if (hasEntryPermissions()) {
            maybeStartWatchdog()
        } else {
            requestPermissions(requiredPermissions(), REQUEST_CODE)
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning from Settings
        refreshScannerState()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CODE) {
            return
        }
        val hasFine = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarse = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        Log.d(TAG, "Permissions granted: FINE=$hasFine, COARSE=$hasCoarse")
        refreshScannerState()
    }

    private fun showPermissionDenied() {
        statusView.text =
            "Scanning disabled. Grant Wi-Fi and location permissions in Settings to start."
        configureActionButton("Open App Settings") { openAppSettings() }
    }

    private fun showPreciseLocationRequired() {
        statusView.text =
            "Approximate-only location is degraded. Enable precise location in App Settings to start scanning."
        configureActionButton("Open App Settings") { openAppSettings() }
    }

    private fun openAppSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
        startActivity(intent)
    }

    private fun requiredPermissions(): Array<String> {
        val base =
            mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            base.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        return base.toTypedArray()
    }

    private fun hasEntryPermissions(): Boolean = hasAnyLocationPermission() && hasNonLocationPermissions()

    private fun hasAnyLocationPermission(): Boolean = hasFineLocationPermission() || hasCoarseLocationPermission()

    private fun hasFineLocationPermission(): Boolean = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun hasCoarseLocationPermission(): Boolean = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

    private fun hasNonLocationPermissions(): Boolean =
        requiredPermissions()
            .filter { permission: String ->
                permission != Manifest.permission.ACCESS_FINE_LOCATION &&
                    permission != Manifest.permission.ACCESS_COARSE_LOCATION
            }.all { permission: String ->
                hasPermission(permission)
            }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private var scannerStarted = false

    private fun refreshScannerState() {
        if (hasFineLocationPermission().not() && hasCoarseLocationPermission() && hasNonLocationPermissions()) {
            showPreciseLocationRequired()
            return
        }
        if (hasEntryPermissions()) {
            maybeStartWatchdog()
        } else {
            showPermissionDenied()
        }
    }

    private fun maybeStartWatchdog() {
        if (scannerStarted) return
        if (hasFineLocationPermission().not()) {
            showPreciseLocationRequired()
            return
        }
        if (isDeviceLocationEnabled().not()) {
            showLocationServicesRequired()
            return
        }
        if (requiresBackgroundLocationPrompt()) {
            showBackgroundLocationRequired()
            return
        }
        startWatchdog()
    }

    private fun requiresBackgroundLocationPrompt(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION).not()

    private fun showBackgroundLocationRequired() {
        statusView.text =
            "Field mode needs 'Allow all the time' location access. Without it, locked-screen and background scanning is unreliable."
        configureActionButton("Open App Settings") { openAppSettings() }
    }

    private fun showLocationServicesRequired() {
        statusView.text =
            "Scanning cannot start until device location services are turned on."
        configureActionButton("Open Location Settings") { openLocationSettings() }
    }

    private fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    private fun isDeviceLocationEnabled(): Boolean {
        val locationManager = getSystemService(LocationManager::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF,
            ) != Settings.Secure.LOCATION_MODE_OFF
        }
    }

    private fun configureActionButton(
        text: String,
        onClick: () -> Unit,
    ) {
        actionButton.text = text
        actionButton.setOnClickListener { onClick() }
        actionButton.visibility = View.VISIBLE
    }

    private fun startWatchdog() {
        scannerStarted = true
        statusView.text = "Starting scanner..."
        actionButton.visibility = View.GONE
        val intent = Intent(this, WatchdogService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE = 1001
    }
}
