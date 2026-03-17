package com.wscanplus.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusView =
            TextView(this).apply {
                text = "wscan+ needs Wi-Fi scan permissions to start the scanner."
            }
        setContentView(statusView)

        if (hasAllPermissions()) {
            startWatchdog()
        } else {
            requestPermissions(requiredPermissions(), REQUEST_CODE)
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
        if (hasAllPermissions()) {
            startWatchdog()
        } else {
            statusView.text =
                "Permissions denied. Grant Wi-Fi and location permissions in Settings to start."
        }
    }

    private fun requiredPermissions(): Array<String> {
        val base = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            base.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        return base.toTypedArray()
    }

    private fun hasAllPermissions(): Boolean =
        requiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED
        }

    private fun startWatchdog() {
        statusView.text = "Starting scanner..."
        val intent = Intent(this, WatchdogService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    companion object {
        private const val REQUEST_CODE = 1001
    }
}
