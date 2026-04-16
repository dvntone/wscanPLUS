package com.wscanplus.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import com.wscanplus.app.privacy.ConsentStore

class MainActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var rootLayout: LinearLayout
    private lateinit var actionButton: Button
    private lateinit var settingsButton: Button

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
        settingsButton =
            Button(this).apply {
                text = "Kismet Settings"
                setOnClickListener { openKismetSettings() }
            }
        val mapButton =
            Button(this).apply {
                text = "View Scan Map"
                setOnClickListener { openScanMap() }
            }
        val threatResultsButton =
            Button(this).apply {
                text = "View Threat Results"
                setOnClickListener { openThreatResults() }
            }
        val historyButton =
            Button(this).apply {
                text = "View Scan History"
                setOnClickListener { openScanHistory() }
            }
        val diagnosticButton =
            Button(this).apply {
                text = "Diagnostics"
                setOnClickListener { openDiagnostics() }
            }
        rootLayout.addView(statusView)
        rootLayout.addView(actionButton)
        rootLayout.addView(settingsButton)
        rootLayout.addView(mapButton)
        rootLayout.addView(threatResultsButton)
        rootLayout.addView(historyButton)
        rootLayout.addView(diagnosticButton)
        setContentView(rootLayout)

        if (!ConsentStore(this).isConsentGiven()) {
            showConsentDialog()
        } else if (hasEntryPermissions()) {
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
        Log.i(TAG, "Permissions granted: FINE=$hasFine, COARSE=$hasCoarse")
        refreshScannerState()
    }

    private fun showPermissionDenied() {
        statusView.text =
            "Scanning disabled. Grant Wi-Fi and location permissions in Settings to start."
        configureActionButton("Open App Settings") { openAppSettings() }
    }

    private fun showPreciseLocationRequired() {
        statusView.text =
            "Precise location is required for Wi-Fi scanning. Enable it in App Settings."
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
        if (hasFineLocationPermission().not()) {
            showPreciseLocationRequired()
            return
        }
        if (isDeviceLocationEnabled().not()) {
            showLocationServicesRequired()
            return
        }
        // Background location missing: start in degraded mode (no GPS sampler, limited to
        // foreground sessions). Scanner chain still runs with ACCESS_FINE_LOCATION.
        val degraded = requiresBackgroundLocationPrompt()
        startWatchdog(degraded = degraded)
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

    private fun openKismetSettings() {
        startActivity(Intent(this, KismetSettingsActivity::class.java))
    }

    private fun openScanMap() {
        startActivity(Intent(this, ScanMapActivity::class.java))
    }

    private fun openThreatResults() {
        startActivity(Intent(this, ThreatResultsActivity::class.java))
    }

    private fun openScanHistory() {
        startActivity(Intent(this, ScanHistoryActivity::class.java))
    }

    private fun openDiagnostics() {
        startActivity(Intent(this, DiagnosticActivity::class.java))
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

    private fun startWatchdog(degraded: Boolean = false) {
        statusView.text =
            if (degraded) {
                "Starting scanner (degraded — fine location only, without background location)..."
            } else {
                "Starting scanner..."
            }
        actionButton.visibility = View.GONE
        val intent =
            Intent(this, WatchdogService::class.java).apply {
                putExtra(WatchdogService.EXTRA_DEGRADED, degraded)
            }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun showConsentDialog() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Data Sharing Consent")
        dialog.setMessage(
            "wscan+ can submit network data to external threat intelligence services " +
                "(CrowdSec CTI) to enrich threat detection.\n\n" +
                "This is optional. Local Wi-Fi scanning works without consent.",
        )
        dialog.setPositiveButton("Allow") { _, _ ->
            ConsentStore(this).setConsentGiven(true)
            if (hasEntryPermissions()) {
                maybeStartWatchdog()
            } else {
                requestPermissions(requiredPermissions(), REQUEST_CODE)
            }
        }
        dialog.setNegativeButton("Decline") { _, _ ->
            ConsentStore(this).setConsentGiven(false)
            if (hasEntryPermissions()) {
                maybeStartWatchdog()
            } else {
                requestPermissions(requiredPermissions(), REQUEST_CODE)
            }
        }
        dialog.setCancelable(false)
        dialog.show()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE = 1001
    }
}
