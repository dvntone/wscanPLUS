package com.wscanplus.app.capabilities

import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.provider.Settings

object DeviceLocationState {
    fun isLocationEnabled(context: Context): Boolean {
        val appContext = context.applicationContext
        val locationManager = appContext.getSystemService(LocationManager::class.java)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager?.isLocationEnabled ?: false
        } else {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(
                appContext.contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF,
            ) != Settings.Secure.LOCATION_MODE_OFF
        }
    }
}
