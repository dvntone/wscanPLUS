package com.wscanplus.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
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
                visibility = Button.GONE
            }
        rootLayout.addView(statusView)
        rootLayout.addView(actionButton)
        setContentView(rootLayout)

        if (hasAllPermissions()) {
            maybeStartWatchdog()
        } else {
            requestPermissions(requiredPermissions(), REQUEST_CODE)
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning from Settings
        if (hasAllPermissions()) {
            maybeStartWatchdog()
        }
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
        if (hasAllPermissions()) {
            maybeStartWatchdog()
        } else {
            showPermissionDenied()
        }
    }

    private fun showPermissionDenied() {
        statusView.text =
            "Scanning disabled. Grant Wi-Fi and location permissions in Settings to start."
        configureActionButton("Open Settings") { openAppSettings() }
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

    private fun hasAllPermissions(): Boolean = hasRequiredLocationPermission() && hasNonLocationPermissions()

    private fun hasRequiredLocationPermission(): Boolean =
        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)

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

    private fun maybeStartWatchdog() {
        if (requiresBackgroundLocationPrompt()) {
            showBackgroundLocationRequired()
            return
        }
        if (scannerStarted) return
        startWatchdog()
    }

    private fun requiresBackgroundLocationPrompt(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
            hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION).not()

    private fun showBackgroundLocationRequired() {
        scannerStarted = false
        statusView.text =
            "Background and lock-screen scanning requires 'Allow all the time' location access."
        configureActionButton("Open Settings") { openAppSettings() }
    }

    private fun configureActionButton(
        text: String,
        onClick: () -> Unit,
    ) {
        actionButton.text = text
        actionButton.setOnClickListener { onClick() }
        actionButton.visibility = Button.VISIBLE
    }

    private fun startWatchdog() {
        scannerStarted = true
        statusView.text = "Starting scanner..."
        actionButton.visibility = Button.GONE
        val intent = Intent(this, WatchdogService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE = 1001
    }
}
