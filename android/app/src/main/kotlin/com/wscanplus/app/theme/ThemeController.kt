package com.wscanplus.app.theme

import androidx.appcompat.app.AppCompatDelegate

object ThemeController {
    fun apply(preference: ThemePreference) {
        AppCompatDelegate.setDefaultNightMode(
            when (preference) {
                ThemePreference.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemePreference.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemePreference.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            },
        )
    }
}

